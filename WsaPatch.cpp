#include "WsaPatch.h"

#include <string>
#include <string_view>

#include <windows.h>

#include "Log.h"
#include "ErrnoRestorer.h"
#include "TimeUtils.h"

static constexpr auto LOG_TAG = L"WsaPatch";

namespace wsapatch {

bool kDebug = false;

HINSTANCE gSelfInstance = nullptr;

HANDLE gConsoleOutput = INVALID_HANDLE_VALUE;
bool gConsoleIsAllocated = false;

HMODULE hNtdll = nullptr;
HMODULE hWsaClient = nullptr;

RTL_OSVERSIONINFOEXW gOsVersionInfo = {0};
bool gIsPatchVersionNumber = false;
bool gIsPatchProductType = false;

#define NT_SUCCESS(Status) (((NTSTATUS)(Status)) >= 0)
#define STATUS_SUCCESS ((NTSTATUS)0x00000000L)

void defaultLogHandler(Log::Level level, const WCHAR *tag, const WCHAR *msg) {
    HANDLE handle = gConsoleOutput;
    if (handle == nullptr || handle == INVALID_HANDLE_VALUE) {
        return;
    }
    WCHAR buffer[1024] = {};
    u64 now = currentTimeMillis();
    const WCHAR *levelName = Log::levelName(level);
    // MM-dd HH:mm:ss.SSS [level] tag: msg
    int month = 0, day = 0, hour = 0, minute = 0, second = 0, millisecond = 0;
    timeMillisToLocalCalendar(now, nullptr, &month, &day, &hour, &minute, &second, &millisecond);
    StringCchPrintfW(buffer, _countof(buffer),
                     L"%02d-%02d %02d:%02d:%02d.%03d %c %s: %s\n",
                     month, day, hour, minute, second, millisecond,
                     WCHAR(*levelName), tag, msg);
    DWORD written = 0;
    WriteConsoleW(handle, buffer, static_cast<DWORD>(wcslen(buffer)), &written, nullptr);
}

using FuncRtlGetVersion = NTSYSAPI NTSTATUS(*)(PRTL_OSVERSIONINFOW lpVersionInformation);

NTSTATUS WINAPI FakeRtlGetVersion(PRTL_OSVERSIONINFOW lpVersionInformation) {
    // The minimal version is 10.0.22000.120 VER_NT_WORKSTATION
    LOGD(L"-FakeRtlGetVersion");
    DWORD size = lpVersionInformation->dwOSVersionInfoSize;
    memcpy(lpVersionInformation, &gOsVersionInfo, size);
    lpVersionInformation->dwOSVersionInfoSize = size;
    if (gIsPatchVersionNumber) {
        lpVersionInformation->dwBuildNumber = 22000;
    }
    if (gIsPatchProductType && size >= sizeof(OSVERSIONINFOEXW)) {
        ((PRTL_OSVERSIONINFOEXW) lpVersionInformation)->wProductType = VER_NT_WORKSTATION;
    }
    return STATUS_SUCCESS;
}

FARPROC WINAPI BadGetProcAddress(_In_ HMODULE hModule, _In_ LPCSTR lpProcName) {
    FARPROC result;
    if (hModule == hNtdll && lpProcName != nullptr && strcmp(lpProcName, "RtlGetVersion") == 0) {
        result = reinterpret_cast<FARPROC>(FakeRtlGetVersion);
        SetLastError(0);
    } else {
        result = GetProcAddress(hModule, lpProcName);
        if (result == nullptr) {
            ErrnoRestorer errnoRestorer;
            if (hModule != nullptr) {
                WCHAR buffer[1024] = {};
                GetModuleFileNameW(hModule, buffer, _countof(buffer));
                LOGW(L"-GetProcAddress: hModule=%s(%p), lpProcName=%hs, result=NULL", buffer, hModule, lpProcName);
            } else {
                LOGW(L"-GetProcAddress: hModule=NULL, lpProcName=%hs, result=NULL", lpProcName ? lpProcName : "NULL");
            }
        }
    }
    return result;
}

usize kPageSize = 0;

bool isAddressReadable(void *address) {
    if (kPageSize == 0) {
        SYSTEM_INFO si = {};
        GetSystemInfo(&si);
        kPageSize = si.dwPageSize;
    }
    void *pageBase = reinterpret_cast<void *>(reinterpret_cast<uintptr>(address) & ~(kPageSize - 1));
    MEMORY_BASIC_INFORMATION info = {};
    if (VirtualQuery(pageBase, &info, sizeof(info)) == 0) {
        return false;
    }
    return (info.Protect & PAGE_READONLY) != 0 || (info.Protect & PAGE_READWRITE) != 0;
}

int HookIATProcedure(HMODULE hModule, LPCSTR procName, FARPROC replacement) {
    if (hModule == nullptr || procName == nullptr || replacement == nullptr) {
        LOGE(L"HookIATProcedure: invalid arguments: hModule=%p, procName=%hs, replacement=%p",
             hModule, procName ? procName : "(NULL)", replacement);
        return 0;
    }
    LOGI(L"HookIATProcedure: start, hModule=%p, procName=%hs, replacement=%p", hModule, procName, replacement);
    PIMAGE_DOS_HEADER pDosHeader = (PIMAGE_DOS_HEADER) hModule;
    PIMAGE_NT_HEADERS pNtHeaders = (PIMAGE_NT_HEADERS) ((BYTE *) hModule + pDosHeader->e_lfanew);
    PIMAGE_IMPORT_DESCRIPTOR pImportDesc = (PIMAGE_IMPORT_DESCRIPTOR)
            ((BYTE *) hModule + pNtHeaders->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT].VirtualAddress);
    int count = 0;
    for (; pImportDesc->Name != 0; pImportDesc++) {
        PIMAGE_THUNK_DATA pThunk = (PIMAGE_THUNK_DATA) ((BYTE *) hModule + pImportDesc->FirstThunk);
        PIMAGE_THUNK_DATA pOrgThunk = (PIMAGE_THUNK_DATA) ((BYTE *) hModule + pImportDesc->OriginalFirstThunk);
        for (; pThunk->u1.Function != 0; pThunk++, pOrgThunk++) {
            if (IMAGE_SNAP_BY_ORDINAL(pOrgThunk->u1.Ordinal)) {
                continue;
            }
            PIMAGE_IMPORT_BY_NAME pImport = (PIMAGE_IMPORT_BY_NAME) ((BYTE *) hModule + pOrgThunk->u1.AddressOfData);
            if (!isAddressReadable((char *) pImport->Name)) {
                LOGE(L"HookIATProcedure: pImport->Name is not readable: %p", pImport->Name);
                WCHAR buffer[1024] = {};
                GetModuleFileNameW(hModule, buffer, _countof(buffer));
                LOGE(L"HookIATProcedure: abort IAT hook, hModule=%s(%p), procName=%hs, replacement=%p",
                     buffer, hModule, procName, replacement);
                return -1;
            }
            if (strcmp((char *) pImport->Name, procName) == 0) {
                LOGI(L"HookIATProcedure: Found %hs at %p", procName, pThunk);
                DWORD oldProtect = 0;
                VirtualProtect(pThunk, sizeof(PVOID), PAGE_READWRITE, &oldProtect);
                pThunk->u1.Function = (DWORD_PTR) replacement;
                VirtualProtect(pThunk, sizeof(PVOID), oldProtect, &oldProtect);
                count++;
            }
        }
    }
    LOGI(L"HookIATProcedure: end, count=%d", count);
    return count;
}

void checkEnableDebugConsole() {
    WCHAR exePath[1024] = {};
    if (GetModuleFileNameW(nullptr, exePath, 1024) != 0) {
        std::wstring path = exePath;
        auto dirLen = path.find_last_of('\\');
        if (dirLen == std::wstring::npos) {
            dirLen = 0;
        }
        std::wstring enableDebugConsole = path.substr(0, dirLen) + L"\\EnableDebugConsole";
        if (GetFileAttributesW(enableDebugConsole.c_str()) != INVALID_FILE_ATTRIBUTES) {
            kDebug = true;
        }
    }
}

bool OnLoad(HINSTANCE hInstDLL) {
    gSelfInstance = hInstDLL;
    hNtdll = GetModuleHandleW(L"ntdll.dll");
    if (hNtdll == nullptr) {
        return false;
    }
    checkEnableDebugConsole();
    hWsaClient = GetModuleHandleW(L"WsaClient.exe");
    if (kDebug) {
        if (AllocConsole()) {
            gConsoleIsAllocated = true;
        }
        gConsoleOutput = CreateFileW(L"CONOUT$", GENERIC_WRITE, FILE_SHARE_WRITE, nullptr, OPEN_EXISTING, 0, nullptr);
        if (gConsoleOutput != INVALID_HANDLE_VALUE) {
            Log::setLogHandler(&defaultLogHandler);
        } else {
            std::wstring msg = std::wstring(L"Error CreateFileW CONOUT$, GetLastError=") + std::to_wstring(GetLastError());
            MessageBoxW(nullptr, msg.c_str(), L"wsapatch.dll", MB_OK | MB_ICONERROR);
        }
    }
    LOGD(L"OnLoad, hInstDLL=%p, hNtdll=%p, hWsaClient=%p", hInstDLL, hNtdll, hWsaClient);
    if (hWsaClient == nullptr) {
        // check if we are loaded into the correct process
        WCHAR filename[MAX_PATH];
        GetModuleFileNameW(nullptr, filename, MAX_PATH);
        // get lower case filename
        for (int i = 0; filename[i] != 0; i++) {
            filename[i] = towlower(filename[i]);
        }
        if (wcsstr(filename, L"\\wsaclinent.exe") == nullptr) {
            WCHAR buf[1024] = {};
            StringCbPrintfW(buf, 1024, L"GetModuleHandleW(L\"WsaClient.exe\") is NULL.\nIs wsapatch.dll loaded into wrong process?\n%s", filename);
            MessageBoxW(nullptr, buf, L"wsapatch.dll", MB_OK | MB_ICONERROR);
            return false;
        }
        return false;
    }
    LOGD(L"ntdll.dll = %p", hNtdll);
    LOGD(L"WsaClient.exe = %p", hWsaClient);
    FuncRtlGetVersion funcRtlGetVersion = reinterpret_cast<FuncRtlGetVersion>(GetProcAddress(hNtdll, "RtlGetVersion"));
    if (funcRtlGetVersion == nullptr) {
        LOGE(L"GetProcAddress(NTDLL.DLL, \"RtlGetVersion\") failed, GetLastError=%d", GetLastError());
        return false;
    }
    gOsVersionInfo.dwOSVersionInfoSize = sizeof(gOsVersionInfo);
    NTSTATUS status = funcRtlGetVersion(reinterpret_cast<PRTL_OSVERSIONINFOW>(&gOsVersionInfo));
    if (!NT_SUCCESS(status)) {
        LOGE(L"funcRtlGetVersion(&osVersionInfo) failed, status=%d", status);
        return false;
    }
    LOGD(L"RtlGetVersion: dwMajorVersion=%d, dwMinorVersion=%d, dwBuildNumber=%d, dwPlatformId=%d",
         gOsVersionInfo.dwMajorVersion, gOsVersionInfo.dwMinorVersion, gOsVersionInfo.dwBuildNumber, gOsVersionInfo.dwPlatformId);
    bool isWin11 = gOsVersionInfo.dwMajorVersion >= 10 && gOsVersionInfo.dwMinorVersion >= 0 && gOsVersionInfo.dwBuildNumber >= 22000;
    gIsPatchVersionNumber = !isWin11;
    gIsPatchProductType = (gOsVersionInfo.wProductType != VER_NT_WORKSTATION);
    if (!gIsPatchVersionNumber && !gIsPatchProductType) {
        LOGW(L"Windows 11 workstation detected, no need to patch");
        return true;
    }
    LOGD(L"Need to patch, gIsPatchVersionNumber=%d, gIsPatchProductType=%d", gIsPatchVersionNumber, gIsPatchProductType);
    int count = HookIATProcedure(hWsaClient, "GetProcAddress", reinterpret_cast<FARPROC>(&BadGetProcAddress));
    if (count == 0) {
        LOGE(L"HookIATProcedure failed, count=%d", count);
        return false;
    } else {
        LOGI(L"HookIATProcedure success, count=%d", count);
    }
    // auxiliary hooks
    // HookIATProcedure(GetModuleHandleW(L"winhttp.dll"), "GetProcAddress", reinterpret_cast<FARPROC>(&BadGetProcAddress));
    // HookIATProcedure(GetModuleHandleW(L"icu.dll"), "GetProcAddress", reinterpret_cast<FARPROC>(&BadGetProcAddress));
    return true;
}

void OnUnload() {
    HANDLE handle = gConsoleOutput;
    if (handle != nullptr && handle != INVALID_HANDLE_VALUE) {
        CloseHandle(handle);
        if (gConsoleIsAllocated) {
            FreeConsole();
            gConsoleIsAllocated = false;
        }
        gConsoleOutput = INVALID_HANDLE_VALUE;
    }
    gSelfInstance = nullptr;
}

}

EXPORT_C void NS_WsaPatch_UnusedSymbol() {
    // nothing
}

BOOL WINAPI DllMain(HINSTANCE hInstDLL, DWORD fdwReason, LPVOID lpvReserved) {
    // Perform actions based on the reason for calling.
    switch (fdwReason) {
        case DLL_PROCESS_ATTACH:
            return wsapatch::OnLoad(hInstDLL);
        case DLL_THREAD_ATTACH:
        case DLL_THREAD_DETACH:
            // no-op
            break;
        case DLL_PROCESS_DETACH:
            if (lpvReserved != nullptr) {
                // do not do cleanup if process termination scenario
                break;
            }
            wsapatch::OnUnload();
            break;
        default:
            break;
    }
    return true;
}

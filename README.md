# WSA patch for Windows 10

[中文版本](./README_zhs.md)

This is a patch for WSA to enable WSA (Windows Subsystem for Android) to run on Windows 10.

I have tested WSA 2210.40000.7.0 on Windows 10 22H2 10.0.19045.2311 and 2211.40000.10.0 on 10.0.19045.2364.

### Instructions

1. Make sure your Windows version is at least Windows 10 22H2 10.0.19045.2311.
    - You can check your Windows version with command `winver`.
    - If your Windows version is lower than 10.0.19045.2311, please update your Windows to at least 10.0.19045.2311.
2. Get WSA appx zip. You can do this by following instructions in https://github.com/LSPosed/MagiskOnWSALocal
   (You need to "build" this yourself with your local WSL2).
3. Get "icu.dll" from Windows 11 22H2. Note that you MUST use icu.dll from Windows 11.
   The icu.dll from Windows 10 will NOT work.
   (I have made a copy of these DLLs in the original.dll.win11.22h2 dir. They are digitally signed by Microsoft.)
4. Build WsaPatch.dll with source code in this repo.
   (Build with MSVC toolchain, not MinGW or something else.)
5. Patch icu.dll: add WsaPatch.dll as an import DLL as icu.dll.
6. Copy patched icu.dll and WsaPatch.dll to WsaClient dir.
7. Patch AppxManifest.xml.
    1. Find TargetDeviceFamily node in AppxManifest.xml.
       ```xml
       <TargetDeviceFamily Name="Windows.Desktop" MinVersion="10.0.22000.120" MaxVersionTested="10.0.22000.120"/>
       ```

       Change the `MinVersion` from `10.0.22000.120` to `10.0.19045.2311`.

    2. Delete all nodes about "customInstall" extension (see below) in AppxManifest.xml.
       ```xml
       <rescap:Capability Name="customInstallActions"/>
       ```

       ```xml
       <desktop6:Extension Category="windows.customInstall">
           <desktop6:CustomInstall Folder="CustomInstall" desktop8:RunAsUser="true">
               <desktop6:RepairActions>
                   <desktop6:RepairAction File="WsaSetup.exe" Name="Repair" Arguments="repair"/>
               </desktop6:RepairActions>
               <desktop6:UninstallActions>
                   <desktop6:UninstallAction File="WsaSetup.exe" Name="Uninstall" Arguments="uninstall"/>
               </desktop6:UninstallActions>
           </desktop6:CustomInstall>
       </desktop6:Extension>
       ```

8. Run "Run.bat" to register your WSA appx.
9. You should be able to run WSA now.

If you don't want to build WsaPatch.dll and patch icu.dll yourself,
you can download the prebuilt binaries from the [release page](https://github.com/cinit/WSAPatch/releases).

#### About winhttp.dll

- WsaClient.exe does use GetProcAddress to get some functions from winhttp.dll.
- Some functions exist in winhttp.dll of Windows 11 22H2, but not in Windows 10 22H2.
- If you create a file `EnableDebugConsole` in WsaClient directory or set `wsapatch::kDebug` in [WsaPatch.cpp](WsaPatch.cpp) to true,
  you will see the following message from log console.
- If you copy a winhttp.dll from Windows 11 22H2 to WsaClient directory, WsaClient.exe will be able to find these functions.
- WSA will still run even if you don't copy a winhttp.dll with these symbols.

```text
12-10 16:16:29.474 W WsaPatch: -GetProcAddress: hModule=C:\WINDOWS\SYSTEM32\WINHTTP.dll(00007FFC64780000), lpProcName=WinHttpRegisterProxyChangeNotification, result=NULL
12-10 16:16:29.474 W WsaPatch: -GetProcAddress: hModule=C:\WINDOWS\SYSTEM32\WINHTTP.dll(00007FFC64780000), lpProcName=WinHttpUnregisterProxyChangeNotification, result=NULL
12-10 16:16:29.474 W WsaPatch: -GetProcAddress: hModule=C:\WINDOWS\SYSTEM32\WINHTTP.dll(00007FFC64780000), lpProcName=WinHttpGetProxySettingsEx, result=NULL
12-10 16:16:29.474 W WsaPatch: -GetProcAddress: hModule=C:\WINDOWS\SYSTEM32\WINHTTP.dll(00007FFC64780000), lpProcName=WinHttpGetProxySettingsResultEx, result=NULL
12-10 16:16:29.474 W WsaPatch: -GetProcAddress: hModule=C:\WINDOWS\SYSTEM32\WINHTTP.dll(00007FFC64780000), lpProcName=WinHttpFreeProxySettingsEx, result=NULL
```

### Problems I met

1. When using WSA 2209.40000.26.0, I was able to run applications in WSA,
   but I was not able to connect to WSA ADB after enabling Developer Mode,
   since netstat shows that no process is listening on port 58526.
   After I upgraded to WSA 2210.40000.7.0, I was able to connect to WSA ADB.
2. The WSA settings window does not hava a draggable title,
   but you can move the WSA window if you hold the cursor left near the "minimize" button,
   or press Alt+Space, then click "Move" in the context
   menu. [#1](https://github.com/cinit/WSAPatch/issues/1) [#2](https://github.com/cinit/WSAPatch/issues/2)
3. If your WSA crashes(or suddenly disappears) when starting up, try to upgrade your Windows to Windows 10 22H2 10.0.19045.2311.
   (Someone has reported that WSA failed to start on 22H2 19045.2251, but worked after upgrading to 19045.2311.)

If you encounter any problems or have any suggestions, please open an issue or pull request.

### Screenshot

![screenshot](./pic/screenshot_20221202.png)

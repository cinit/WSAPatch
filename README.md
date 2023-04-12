# WSA patch for Windows 10

[中文版本](./README_zhs.md)

This is a patch for WSA to enable WSA (Windows Subsystem for Android) to run on Windows 10.

### Compatibility

Below is a list of versions of WSA and versions of Windows 10 that have been tested together.

| WSA version     | Windows version        |
|-----------------|------------------------|
| 2210.40000.7.0  | 10.0.19045.2311 (22H2) |
| 2211.40000.10.0 | 10.0.19045.2364 (22H2) |
| 2302.40000.9.0  | 10.0.19044.2728 (21H2) |

In theory, this should work with any version of Windows 10 version 2004 (20H1) and above. This is because every update since then has been very minor, and the build number even still shows as 19041 (20H1's build number) in some places, such as the desktop build number watermark if that is enabled.

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

       Change the `MinVersion` from `10.0.22000.120` to `10.0` plus the OS Build number that appears in `winver`. For example, if `winver` shows `Version 21H2 (OS Build 19044.2728)`, you'd change it to `10.0.19044.2728`.

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

### Notice

1. You can only install WSA on a NTFS partition, not on an exFAT partition.
2. You can NOT delete the WSA installation folder.
   What `Add-AppxPackage -Register .\AppxManifest.xml` does is to register an appx package with some existing unpackaged files,
   so you need to keep them as long as you want to use WSA.
   Check https://learn.microsoft.com/en-us/powershell/module/appx/add-appxpackage?view=windowsserver2022-ps for more details.
3. You need to register your WSA appx package before you can run WSA (the 8th step in the instructions).
   For [MagiskOnWSALocal](https://github.com/LSPosed/MagiskOnWSALocal) users, you need to run `Run.bat` in the extracted dir.
   If the script fails, you can take the following steps for diagnosis (admin privilege required):
    1. Open a PowerShell window and change working directory to your WSA directory.
    2. Run `Add-AppxPackage -ForceApplicationShutdown -ForceUpdateFromAnyVersion -Register .\AppxManifest.xml` in PowerShell.
       This should fail with an ActivityID, which is a UUID required for the next step.
    3. Run `Get-AppPackageLog -ActivityID <UUID>` in PowerShell.
       This should print the log of the failed operation.
    4. Check the log for the reason of failure and fix it.

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

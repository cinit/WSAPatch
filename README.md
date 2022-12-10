# WSA patch for Windows 10

[中文版本](./README_zhs.md)

This is a patch for WSA to enable WSA (Windows Subsystem for Android) to run on Windows 10.

I have tested on Windows 10 22H2 10.0.19045.2311 x64 with WSA 2210.40000.7.0.

### Instructions

1. Get WSA appx zip. You can do this by following instructions in https://github.com/LSPosed/MagiskOnWSALocal
   (You need to "build" this yourself with your local WSL2).
2. Get "icu.dll" from Windows 11 22H2. Note that you MUST use icu.dll from Windows 11.
   The icu.dll from Windows 10 will NOT work.
   (I have made a copy of these DLLs in the original.dll.win11.22h2 dir. They are digitally signed by Microsoft.)
3. Build WsaPatch.dll with source code in this repo.
   (Build with MSVC toolchain, not MinGW or something else.)
4. Patch icu.dll: add WsaPatch.dll as an import DLL as icu.dll.
5. Copy patched icu.dll and WsaPatch.dll to WsaClient dir.
6. Patch AppxManifest.xml: Find TargetDeviceFamily node and change the MinVersion attribute to your Windows version.
   <details>

   Find the following line in AppxManifest.xml.
   ```xml
   <TargetDeviceFamily Name="Windows.Desktop" MinVersion="10.0.22000.120" MaxVersionTested="10.0.22000.120"/>
   ```

   Change the `MinVersion` from `10.0.22000.120` to your Windows version, like `10.0.19045.2311`.
   </details>
7. Patch AppxManifest.xml: Delete all nodes about "customInstall" extension in AppxManifest.xml.
   <details>
   Delete the following content from AppxManifest.xml.

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

   </details>
8. Run "Run.bat" to register your WSA appx.
9. You should be able to run WSA now.

If you don't want to build WsaPatch.dll and patch icu.dll yourself,
you can download the prebuilt binaries from the [release page](https://github.com/cinit/WSAPatch/releases).
(They are marked as "pre-release" because I don't know if they are stable enough.)

### Problems I met

1. When using WSA 2209.40000.26.0, I was able to run applications in WSA,
   but I was not able to connect to WSA ADB after enabling Developer Mode,
   since netstat shows that no process is listening on port 58526.
   After I upgraded to WSA 2210.40000.7.0, I was able to connect to WSA ADB.
2. The WSA settings window was not draggable.
   A temporary solution to move WSA settings window is to press Alt+Space, then click "Move" in the context
   menu. [#1](https://github.com/cinit/WSAPatch/issues/1)
3. If your WSA crashes(or suddenly disappears) when starting up, try to upgrade your Windows to Windows 10 22H2 10.0.19045.2311.
   (Someone has reported that WSA failed to start on 22H2 19045.2251, but worked after upgrading to 19045.2311.)

### Screenshot

![screenshot](./pic/screenshot_20221202.png)

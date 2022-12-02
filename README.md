# WSA patch for Windows 10

This is a patch for WSA to enable WSA (Windows Subsystem for Android) to run on Windows 10.

I have tested it with my Windows 10 22H2 x64 with WSA 2210.40000.7.0. Other versions like Windows 10 21H2 is not tested.

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
2. I was unable to drag the WSA settings window(although I can resize the WSA settings window).

### Screenshot

![screenshot](./pic/screenshot_20221202.png)

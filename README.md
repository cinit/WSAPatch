# WSA patch for Windows 10

This is a patch for WSA to enable WSA (Windows Subsystem for Android) to run on Windows 10.

I have tested it with my Windows 10 22H2. Other versions like Windows 10 21H2 is not tested.

Steps:

1. Get WSA appx zip. You can do this by following instructions in https://github.com/LSPosed/MagiskOnWSALocal
   (You need to "build" this yourself with your local WSL2).
2. Get "icu.dll" from Windows 11 22H2. Note that you MUST use icu.dll from Windows 11.
   The icu.dll from Windows 10 will NOT work.
   (I have made a copy of these DLLs in the original.dll.win11.22h2 dir. They are digitally signed by Microsoft.)
3. Build WsaPatch.dll with source code in this repo.
4. Patch icu.dll: add WsaPatch.dll as an import DLL as icu.dll.
5. Copy patched icu.dll and WsaPatch.dll to WsaClient dir.
6. Patch AppxManifest.xml: Find TargetDeviceFamily node and change the MinVersion attribute to your Windows version.
7. Patch AppxManifest.xml: Delete all nodes about "customInstall" extension in AppxManifest.xml.
8. Run "Run.bat" to register your WSA appx.
9. You should be able to run WSA now.

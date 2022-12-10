# 适用于 Windows 10 的 WSA 补丁

[English version](./README.md)

这是一个可以让 Windows 10 运行 WSA (Windows Subsystem for Android) 的补丁及其使用方法。

本人使用的系统版本为 Windows 10 22H2 10.0.19045.2311 x64, 使用的 WSA 版本为 2210.40000.7.0.

### 操作步骤

1. 安装 WSL2 (我用的 Ubuntu 22.04 LTS, 当然你也可以选择你喜欢的发行版，下一步要用到).
2. 按照 https://github.com/LSPosed/MagiskOnWSALocal 的步骤，得到集成了 Magisk 的 WSA AppX 包.
    - 注：你需要在你的 WSL2 里进行操作，WSA 版本建议选 Insider Fast/Dev Channel.
3. 把你的 WSA AppX 包从 WSL2 里复制出来, 它应该是一个压缩包, 解压到你希望安装 WSA 的地方.
    - 位于 `MagiskOnWSALocal/output`, 文件名类似于 `WSA-with-magisk-stable-MindTheGapps_2210.40000.7.0_x64_Release-Nightly.7z`.
4. 用 Visual Studio 编译本仓库的代码，得到 WsaPatch.dll (用 MSVC 工具链编译，如果你不想自己编译，你可以去 release 里下).
5. 想办法从 Windows 11 22H2 的 System32 里复制一个 icu.dll 过来，找一个 PE32+ 编辑工具给它的导入表加一个 WsaPatch.dll.
    - 你必须用 Windows 11 22H2 里的 icu.dll, 因为 Windows 10 的 icu.dll 缺符号;
    - 如果你没有 Windows 11 22H2, 本仓库的 original.dll.win11.22h2 就有原版带微软签名的 icu.dll;
    - 如果你不想自己搞，你可以用 release 里已经修改过的 icu.dll.
6. 把编译好的 WsaPatch.dll 和修改过的 icu.dll 复制到 WSA 的 WsaClient 文件夹.
7. 按以下要求修改 AppxManifest.xml:
    1. 在 `AppxManifest.xml` 找到 `TargetDeviceFamily` 节点，把 `MinVersion` 改成你的 Windows 版本.
       <details>

       找到
       ```xml
       <TargetDeviceFamily Name="Windows.Desktop" MinVersion="10.0.22000.120" MaxVersionTested="10.0.22000.120"/>
       ```

       把 `MinVersion` 从 `10.0.22000.120` 改成你的 Windows 版本，如 `10.0.19045.2311`.
       </details>
    2. 在 `AppxManifest.xml` 删除 "customInstall" 相关节点，一共有两个.
       <details>
       找到以下内容，然后删掉.

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
8. 运行 `Run.bat` (需要管理员权限).

编译好的 WsaPatch.dll 和修改过的 icu.dll 可以从 [release 页面](https://github.com/cinit/WSAPatch/releases) 下载.

### 可能遇到的问题

1. 如果老版本的 WSA 2209.40000.26.0 开了开发者模式也连不上 ADB (端口没有进程监听), 更新到 WSA 2210.40000.7.0 就可以了.
2. WSA 设置窗口无法移动. 但可以通过按快捷键 Alt+Space 然后点弹出菜单里的 "移动". [#1](https://github.com/cinit/WSAPatch/issues/1)
3. 如果你的 WSA 在启动时闪退且没有任何提示，请将 Windows 10 更新到 22H2 10.0.19045.2311 或更高版本.
   (有人反应过 WSA 在 22H2 19045.2251 闪退, 但更新到 22H2 19045.2311 就能用了).

### 截图

![screenshot](./pic/screenshot_20221202.png)

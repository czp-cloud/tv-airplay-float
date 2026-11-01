# AirPlay 投屏小窗（第二版）

Android 8.0+（API 26 起）。这是基于 [jqssun/android-airplay-server](https://github.com/jqssun/android-airplay-server) 的独立 APK 派生项目，包名 `dev.czp.tvairplay`。保留上游 AirPlay/UxPlay 协议、JNI、解码和许可证，新增普通 Android 应用悬浮窗。基线提交为 `a46cca0a6a1274e9e33c0e750f0f1c1eb1d881ea`（上游 0.0.36）。

## 使用

1. 打开电视上的 **AirPlay 投屏小窗**，允许显示在其他应用上层。小米系统如果没有权限设置入口，可由 ADB 对这个包授予 `SYSTEM_ALERT_WINDOW` app-op。
2. 点击 **开启投屏小窗**，再选择 **返回电视节目，保持接收**。没有投屏时不创建可见窗口，后台接收器继续等待连接。
3. iPhone 与电视在同一可发现的局域网中，控制中心 → 屏幕镜像 → **电视投屏小窗**。
4. 收到并解码出视频后自动显示小窗，初始最长边约 320 像素，宽高比跟随实际视频；开始投屏及视频比例改变时自动贴屏幕最右侧，保留所选大小和高度位置并限制在屏幕内。贴边后缩放继续贴边，手动移动仍可自由摆放。默认 100% 不透明。窗口只有画面，没有标题、等待文字或缩放/关闭按钮。断开镜像后完全隐藏，接收器保持可发现，下次投屏自动出现。
5. 手机遥控：浏览器打开控制页显示的 `http://<电视IP>:8765`即可直接控制，当前调试版本免配对码。可用方向键/中央摇杆移动小窗、滑条调大小和透明度、恢复右上角、选择仅电视 / 仅投屏 / 混合声音和调节投屏音量。未连接投屏时也可预设位置和大小，不会强行显示窗口。详见 [WEB_REMOTE.md](WEB_REMOTE.md)。
6. 电视遥控：控制页 → 调整小窗位置 → 方向键移动 → 返回键完成；缩放使用控制页按钮，触摸设备也可直接拖动画面。
7. 临时结束：在 iPhone 停止屏幕镜像，小窗随之隐藏。彻底停止接收：控制页“停止接收并关闭小窗”或通知中的“停止投屏”。

默认电视原声；网页可选择仅投屏（临时独占音频焦点）或混合声音（保留电视原播放器焦点），断开自动回到电视原声。不修改电视媒体音量，不申请摄像头、麦克风、无障碍或 root。ADB 用于安装、授权和测试，运行时无需电脑保持连接。不改默认桌面，不停用电视原有应用。当前按用户要求启用开机后台接收，可在网页/电视控制页关闭；未投屏时不显示窗口。

输入目前仅为控制中心屏幕镜像；不提供独立 AirPlay 视频 URL/HLS 播放。不支持 DRM，不能据手机桌面镜像成功推断微信、受保护视频或所有视频 App 都可镜像。

## 构建

需要 JDK 21、Android SDK Platform 37.0、build-tools 36.0.0、NDK 27.0.12077973、CMake 3.22.1；Gradle 使用上游 wrapper 9.8.1。在忽略的 `local.properties` 中设置 `sdk.dir`，或配置 `ANDROID_HOME`。

```sh
git submodule update --init --recursive
./gradlew --no-daemon :app:assembleDebug -PtargetAbi=armeabi-v7a
```

默认构建适用于当前小米电视的 ARM32 APK，其他架构可通过 `-PtargetAbi=arm64-v8a` 指定。网络无法访问 Maven Central 时可显式加 `-PuseMavenMirror=true`，启用阿里云 Maven 镜像；默认仍使用官方仓库。原生 ExternalProject 下载需配置构建进程的 HTTPS_PROXY（如环境需要）。

输出为 `app/build/outputs/apk/debug/app-debug.apk`。调试签名只用于本地验证；签名密钥、下载缓存、设备证据和本地配置均不进入 Git。

## 改动位置

- `floating/FloatControlActivity.kt`：电视遥控器控制页。
- `floating/MirrorOverlay.kt` / `OverlayGeometry.kt`：`TYPE_APPLICATION_OVERLAY`、TextureView、按视频比例拖动/缩放、边界及位置和最长边持久化、Surface 生命周期。
- `renderer/VideoRenderer.kt`：每次解码器首次输出真实视频帧时通知服务显示小窗，避免只协商连接就出现空窗口。
- `renderer/VideoPipeline.kt`：按源视频宽高比在浮窗内留边，避免竖屏镜像拉伸。
- `service/AirPlayService.kt`：接收服务拥有浮窗；连接不启动全屏界面；窗口/网页与音频生命周期统一管理；忽略发送端修改电视音量。
- `remote/`：NanoHTTPD 局域网控制服务、主线程命令与音频焦点策略；`assets/remote/` 是手机优先网页。
- `Prefs.kt`：默认 720p / 30fps、H.264、手动启动、单会话策略、保留已验证的上游兼容性广播；独立视频 URL 播放未启用。
- 接收器使用每次安装生成、持久保存的独立标识，避免与电视自带 AirPlay 服务共用硬件 MAC 而被 iPhone 合并显示；不改变网络 MAC 或路由设置。
- `floating/OverlayCommandsReceiver.kt`：仅供系统 DUMP 权限调用的诊断控制，不对普通 App 开放。

安装/运行情况与边界记录在本地 `artifacts/`；未实测的内容不视为已通过。当前小米电视已由用户现场确认 iPhone 镜像可叠加在 HDMI 盒子画面上；不据此承诺所有电视、商业综艺、4K/HDR 或长期待机兼容。

## 撤销

在控制页停止接收，再从电视设置卸载本应用即可；如仅撤销悬浮窗授权，可将这个包的 `SYSTEM_ALERT_WINDOW` app-op 恢复到安装前值。没有系统包、桌面或路由器配置需要还原。

## 许可

本派生项目按原仓库 GPL-3.0 许可证提供，保留根目录 LICENSE 与上游声明；新增依赖说明在 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。原生依赖仍适用各自许可证。分发修改版 APK 时应按许可证提供对应源码、子模块版本与构建说明；当前没有推送或对外发布。

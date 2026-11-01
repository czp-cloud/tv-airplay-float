# AirPlay 投屏小窗

无需 root 的 Android TV 悬浮投屏接收器。用 iPhone 控制中心的“屏幕镜像”连接电视，在继续看电视或 HDMI 盒子时，把手机画面放进可移动、可缩放的小窗，并通过手机网页控制。

这是独立维护的 APK，包名 `dev.czp.tvairplay`。代码基于 [jqssun/android-airplay-server](https://github.com/jqssun/android-airplay-server) 二次开发，保留上游 Git 历史和 GPL-3.0 许可证；AirPlay 协议和原生依赖继续使用上游的 UxPlay 等实现。它与 CleanYourTV 清广告/桌面项目分开，不依赖后者运行。

## 第一个发布版本

`v0.3.3` 是本项目首个发布里程碑，对应应用版本 `0.3.3-opacity`、versionCode `42`。此前的提交记录保留开发过程，并不表示已经对外发布过多个版本。

- 接收 iPhone 控制中心的屏幕镜像，后台等待连接；解码出画面后显示，断开后隐藏。
- 画面窗口跟随横竖屏比例，每次开始投屏和改变比例后自动贴屏幕最右边。
- 手机网页提供摇杆、窗口大小、透明度、独立投屏音量和开机接收开关，无需配对码。
- 声音支持仅电视、仅投屏、混合声音；本台小米电视的 HDMI 原声与投屏同时播放已通过现场试听。
- 窗口只有视频，没有标题栏和最大化/最小化按钮；位置、大小、透明度和投屏音量会保存。

## 使用

1. 安装 APK，打开“AirPlay 投屏小窗”，允许显示在其他应用上层并开启接收。
2. 返回电视节目或 HDMI 输入，让 iPhone 与电视连接同一局域网。
3. iPhone 控制中心 → 屏幕镜像 → “电视投屏小窗”。
4. 手机浏览器打开 `http://<电视IP>:8765`，移动小窗或调整大小、透明度和声音。
5. 在 iPhone 停止屏幕镜像即可隐藏小窗；接收服务继续等待下次连接。

透明度数值越低越透明，100% 为完全不透明。“恢复右上角”同时恢复完全不透明。开机自动接收保持开启，启动服务不弹等待窗口；可以在网页或电视控制页关闭。

详细操作与实现见 [FLOATING.md](FLOATING.md) 和 [WEB_REMOTE.md](WEB_REMOTE.md)。变更记录见 [CHANGELOG.md](CHANGELOG.md)。

## 兼容与验证

- 最低 Android 8.0 / API 26，当前安装包面向 ARM32 `armeabi-v7a`。
- 现场设备：小米 MiTV-MTEQ0，Android 10。已确认 iPhone 镜像可覆盖 HDMI 输入，移动、缩放、三种声音模式、横竖屏和自动靠右正常。
- 透明度网页与真实电视 HTTP 已检查；现场视觉效果未单独重新验收。安装后自动启动和模拟快速开机已通过，真正重开后的自启按用户要求尚未验证。
- 不支持 DRM；不承诺所有商业视频 App、其他电视、音响直通、4K/HDR 或长期待机兼容。当前入口是屏幕镜像，独立 AirPlay 视频 URL/HLS 播放没有启用。

验证记录：[混音与自启](VALIDATION_V3.md)、[横竖屏与靠右](VALIDATION_V3_1.md)、[透明度](VALIDATION_V3_3.md)。

## 构建

需要 JDK 21、Android SDK Platform 37.0、build-tools 36.0.0、NDK 27.0.12077973 和 CMake 3.22.1。Gradle wrapper 已包含在仓库中。

```sh
git clone --recurse-submodules ssh://git@192.168.2.5:2222/czppp/tv-airplay-float.git
cd tv-airplay-float
# 配置 ANDROID_HOME，或在忽略的 local.properties 中设置 sdk.dir
./gradlew --no-daemon :app:assembleDebug -PtargetAbi=armeabi-v7a
```

输出为 `app/build/outputs/apk/debug/app-debug.apk`。其他 ABI 可指定 `-PtargetAbi=arm64-v8a`；依赖下载镜像和完整工具链说明见 [FLOATING.md](FLOATING.md)。本地签名密钥、SDK、缓存、APK、设备日志和家庭截图不进入源码仓库。

## 上游与许可证

- 接收器基线：[android-airplay-server @ a46cca0](https://github.com/jqssun/android-airplay-server/tree/a46cca0a6a1274e9e33c0e750f0f1c1eb1d881ea)，上游版本 0.0.36；原始说明可在该提交查看。
- AirPlay 原生实现：[UxPlay](https://github.com/FDH2/UxPlay)，其余原生依赖通过 Git submodule 固定版本。
- 本项目保留 [GPL-3.0 许可证](LICENSE)；新增依赖声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

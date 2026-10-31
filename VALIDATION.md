# 小米电视实机验证

验证日期：2026-10-31。设备为 Xiaomi MiTV-MTEQ0 / baymax，Android 10 / API 29，ARM32，user 构建，SELinux Enforcing。使用已授权的普通 ADB shell；未使用 root、su、刷机或系统包修改。

## 当前构建

- 上游基线 `a46cca0a6a1274e9e33c0e750f0f1c1eb1d881ea`，保留上游 UxPlay 补丁与 GPL-3.0 许可证。
- JDK 21、Gradle 9.8.1、AGP 9.4.1、SDK 37.0、build-tools 36.0.0、NDK 27.0.12077973、CMake 3.22.1。
- `:app:assembleDebug :app:lintDebug -PtargetAbi=armeabi-v7a -PuseMavenMirror=true` 通过。
- Lint：0 errors、61 warnings、1 hint，未用 baseline 隐藏错误。
- 包名 `dev.czp.tvairplay`，版本 `0.1.0-airplay-float`，min API 26，target API 37，仅 ARM32 原生库。
- APK 29,804,660 字节，签名验证通过。
- APK SHA-256：`7f9d4358ccf7468de324c0e916e83ea9b494bd869e87bc3ba9af70376569b019`。

## 已确认的真实镜像

前一版小窗由用户现场确认：iPhone 控制中心可发现“电视投屏小窗”，连接后可显示实际手机画面，且可以叠加在 HDMI 电视盒子输入上。日志记录多个连接/断开与横竖屏尺寸变化（334×720、332×720、1280×592、1280×720），实际使用 `OMX.MS.AVC.Decoder` 解码 H.264。

发现故障已修复：自带 AirPlay 服务与本应用原先共用 TV 硬件 MAC 作为 deviceid，iPhone 只显示自带接收器；改为独立、持久的应用接收器标识后，用户确认可发现并连接。没有更改网卡 MAC 或局域网配置。

在 HDMI 模式下，Android 截图可能没有包含悬浮层；WindowManager 状态和截图不能代替用户看到的物理屏幕。HDMI 可见性结论来自用户现场确认。

## 历史检查

- 初版 `artifacts/device-smoke/report.json`：16 个断言通过，覆盖安装、普通 UID、悬浮窗权限、移动缩放边界、退出控制页保留底层焦点、音量读数、停止及重启。
- 半透明版本 `artifacts/compact-recheck/report.json`：8 个断言通过。更早一次 compact-smoke 的音量等值断言失败；系统音频事件同时记录两次来自 WindowManager 的音量加键。保留失败报告，之后重测音量不变。不能把一次瞬时音量等值检查解释为永久音频隔离证明。
- 当前版本改为默认 100% 不透明、320×180 无标题纯画面；无镜像时不添加显示窗口，真实解码输出后自动显示，镜像结束或最后连接断开后移除窗口，服务保持运行。接收服务不依赖可见窗口。

## 当前版本设备验证

16:38:17–16:38:30（Asia/Shanghai），后台安装并运行 `tools/device_smoke.py --compact-only`，11 个断言全部通过：型号、安装前无镜像会话、远端 APK SHA-256、安装成功、悬浮窗授权、接收服务 RUNNING、空闲窗口隐藏、320×180 无标题尺寸、opacity=1、前台焦点不变、音量读数不变。

前台始终为 `com.xiaomi.mitv.tvplayer/.ExternalSourceActivity`，未打开控制页或切换输入。报告在 `artifacts/frameless-smoke/report.json`。

16:40:11–16:40:13，观察到真实 iPhone 镜像会话：334×720，H.264 厂商硬件解码，小窗自动显示，opacity=1、chrome=false；显示 Surface 更新计数由 21 增至 68。手机断开后 connections=0、mirroring=false、visible=false，系统窗口列表已无 AirPlayMirrorOverlay，接收服务仍为 RUNNING。此过程的运行时自动显示/隐藏检查通过。

证据保存在 `artifacts/frameless-session/states.json` 与 `receiver.log`。计数只证明此次会话实际有画面更新，不作为帧率或延迟测量。新版亮度和无按钮外观的物理屏幕观感仍以用户现场反馈为准。

## 边界

- 未测持续帧率、端到端延迟、长时间稳定性、微信视频、DRM、4K/HDR、待机恢复或所有商业综艺 App；实际桌面镜像与 HDMI 成功不代表这些均可用。
- 第一版固定静音：不打开本地镜像音频输出，不修改电视媒体音量。通话音频路由和回声未验证。
- 没有自启、不替换默认桌面、不停用电视已有应用。
- 私人设备日志与截图保存在忽略的 `artifacts/`，不提交或发布。

## 复测

安装主机依赖 `adb-shell==0.4.4` 后，可使用后台检查：

```sh
python3 tools/device_smoke.py --host <电视IP> --expected-model MiTV-MTEQ0 --compact-only
```

后台检查核对空闲窗口隐藏、纯画面尺寸、完全不透明、接收服务、前台焦点与音量。成功后保持接收器运行；不打开控制 Activity。安装可加 `--apk <APK>`，会先拒绝覆盖正在投屏的会话；分块上传后加 `--already-uploaded`，仍核对 SHA-256。完整检查（不带 compact-only）会打开控制页并操作遥控返回，仅在用户不看电视时运行。真实 AirPlay 自动显示与隐藏还需 iPhone 实际连接、滑动屏幕、停止镜像。

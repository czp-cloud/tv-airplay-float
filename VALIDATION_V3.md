# 0.3.0：三种声音模式与后台自启

验证日期：2026-11-01，Asia/Shanghai。目标仍为 MiTV-MTEQ0 / baymax，Android 10 / API 29，普通应用 UID 10051。

## 构建与部署

- 版本 0.3.0-mixed-audio，versionCode 39，ARM32。独立包名和签名不变。
- assembleDebug / lintDebug 成功；Lint 0 errors、65 warnings、1 hint。
- APK 29,777,857 字节，签名校验通过，SHA-256 `a0691fe92038c1bd46eed3f428e986f0c7f6ab06cdc0562c21eb2d1979603e1f`。保存于忽略的 artifacts/releases/tv-airplay-float-0.3.0-debug.apk。
- 用户重新开启 ADB 后已核对型号；安装前确认无镜像会话，607 块上传验证 MD5、远端 SHA-256，后台安装成功。
- 安装时由系统真实发送 MY_PACKAGE_REPLACED，BootReceiver 自动启动服务；没有另行调用 start 才使安装检查通过。09:45:54 收到事件、09:45:54.965 网页开始监听、随后 mDNS 注册成功。
- 安装检查 9 项通过：型号、无镜像、SHA、安装、自动启动、空闲无窗口、直接网页 API、自启设置，以及前台保持不变。证据 artifacts/v3-install/。

## 浏览器与实机 HTTP

Chrome / WebKit 共 66 个 fixture 浏览器检查通过：三模式点选、键盘导航、混合时显示投屏音量、自启开关、此前的免配对、手机排版、摇杆、音量、缩放与断线恢复。证据 artifacts/web-v3-smoke/；这不等同于实机声音已经通过。

电视实际 HTTP 接口 36 项测试通过，覆盖 tv / mirror / mixed 的空闲选择、无会话不抢焦点/输出、0/35/100% PCM 增益映射、自启开关写入及参数拒绝，测试后恢复原窗口、投屏音量与开启自启。报告见 artifacts/v3-http-smoke.json。未使用全局静音或写电视媒体音量。

## 现场音频配置

只读采集到内置 Speaker 支持 2 声道、48 kHz；另有 SPDIF 输出。厂商 audio_policy_configuration.xml 中 Speaker、SPDIF 的 route 类型为 mix，来源包括 primary output 与 HDMI In。AudioFlinger 有正常双声道软件 mixer，原生 HAL 标识 MTK/MStar。这里只能证明配置具备混合路线，仍须实际试听。

## 启动验证边界

- 自启偏好 boot_auto_start 已开启，BootReceiver 组件已在 manifest 启用；用户可在网页/电视页关闭。
- 真实系统 MY_PACKAGE_REPLACED 触发自动后台启动已通过。
- 模拟厂商 QUICKBOOT_POWERON：关闭偏好后不启动；重新开启后可启动前台接收服务和网页，且无等待小窗和音频输出。artifacts/v3-boot-smoke.json 的 5 项断言通过。
- ADB shell 发送标准 BOOT_COMPLETED 被 Android 10 以 protected-broadcast 权限拒绝，因此模拟快速开机不能冒充真正冷启动。尚未重启电视验证系统实际 BOOT_COMPLETED 投递和厂商自启策略。
- 接收阶段只提升 connectedDevice 前台服务类型；实际播放投屏声音才加入 mediaPlayback。未测更新 Android 版本的启动行为。

## 混音现场结果

等待用户试听电视 HDMI 有声内容与 iPhone 有声镜像，并记录 mixed 模式的焦点栈、音频流、TV HAL mute 状态和主观听感。当前不根据输出流打开或配置中的 mix 字段宣称两路均可听。

所有设备日志/截图在忽略的 artifacts；不提交家庭媒体内容。

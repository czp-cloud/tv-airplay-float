# 第二版：手机网页遥控器

当前版本 `0.3.2-right-edge`，versionCode 41；横竖屏与贴右边行为验证见 VALIDATION_V3_1.md，混音与自启记录见 VALIDATION_V3.md。HTTP 服务随电视 APK 运行，端口 8765；不需要电脑代理网页、云服务或外网资源。遥控的是本应用的小窗，不控制其他 App 或 HDMI 盒子的遥控按键。

## 手机使用

1. 在电视打开 AirPlay 投屏小窗并开启接收。控制页显示局域网地址。
2. 手机连接电视所在局域网，打开 `http://<电视IP>:8765`即可控制；按用户要求，当前调试版本不需要配对码或登录。
3. 方向按钮点按微调、长按连续移动；也可拖动中央摇杆斜向移动。松手、触摸取消、页面失焦或切到后台后停止连续移动。
4. 窗口外框跟随投屏视频宽高比，竖屏窄长、横屏宽；滑条调整最长边，百分比仍以电视屏幕宽度作参照。开始投屏和视频比例改变时自动贴到屏幕最右侧，保留所选最长边和高度位置，仅在越界时调整高度；已贴边时缩放也保持贴边，摇杆可手动移开。宽高均最多占屏幕对应方向的 80%。若竖屏暂时容纳不了所选大小，转回横屏后恢复原先大小。可一键恢复右上角最长边 320 像素小窗；没有投屏时保留上一次比例用于预览。
5. 声音提供三个单选项：仅电视、仅投屏、混合声音。默认仅电视，断开镜像后回到仅电视；没有镜像/音频格式时不打开输出。仅投屏申请临时独占焦点，混合声音保留电视原播放器焦点，让本应用共享 AAudio 输出与电视声音共存。选择仅投屏或混合后会显示 0–100% 音量滑条，0 为静音、100 为原始幅度；控制的是本应用 PCM 增益，保留电视媒体音量，设置会保存。退出网页不停止电视接收。
6. “开机自动接收”已默认开启，可从手机网页或电视控制页关闭。启动时只运行接收服务与网页，不弹 Activity、不抢焦点、不显示等待小窗；不自动切到投屏/混音。

页面手机优先：320/375/390/430 像素竖屏单列，平板/电脑自动双列；大触控区、原生滑条、减少动态效果偏好、键盘操作与断线重连提示。预览是位置示意，不采集或传输电视截图。

## 实现

- `RemoteControlServer.kt`：复用 [NanoHTTPD 2.3.1](https://github.com/NanoHttpd/nanohttpd)，四线程有界并发、5 秒 socket 超时；小型 JSON 请求、固定资源白名单，窗口和音频操作由 Handler 派发至主线程，2 秒超时的未执行任务会取消。
- 当前开发版直接开放局域网控制，静态资源和 API 都不要求配对码或 Bearer token。保留局域网来源、本机数字 IP Host 和 Origin 校验，不启用 CORS，状态不缓存，不允许 iframe 嵌入；未修改路由器或开放互联网端口。此前配对版仍保留在 Git 历史。
- `GET /api/state`：接收、镜像、窗口与音频状态。`POST /api/move {dx,dy}`、`/api/size {longEdge}`、`/api/reset {}`、`/api/audio {source:"tv"|"mirror"|"mixed"}`、`/api/volume {volume:0..100}`、`/api/autostart {enabled:boolean}`。size 兼容旧的单字段 `{width}`，仍表示像素宽度；禁止同时传两种尺寸。窗口状态增加 aspectRatio、longEdge、preferredLongEdge、minLongEdge、maxLongEdge，网页据此同步预览和滑条边界。未知参数、浮点移动、越界输入、错误方法会拒绝。没有 shell、任意文件读取或对其他 App 的控制接口。
- `MirrorOverlay.kt` / `OverlayGeometry.kt`：按实际视频比例计算窗口宽高，持久保存所选最长边；空闲时调整设置不添加可见窗口，保留第一版真实视频后显示、断开隐藏。旧安装的宽度迁移为所选最长边。
- `MirrorAudioController.kt`：保存独立投屏音量并传给 Oboe PCM 输出增益；仅在真实镜像会话且音频格式已到达时启动声音；仅投屏申请临时独占焦点，混合模式不夺取 TV 焦点。复用已有 AAC/ALAC → TimelineBuffer → Oboe 音频链；仅电视、断开、独占模式永久焦点丢失或服务销毁时关闭输出并放弃焦点。短暂丢失焦点暂停本地声音，得到焦点后恢复。失败时退回电视原声；不通过全局静音伪造源切换。
- Oboe 使用 MEDIA usage，与音频焦点属性一致。第一版没有开启的音频输出按用户选择启用；系统媒体音量与发送端音量同步仍不改动。

BootReceiver 处理 BOOT_COMPLETED、厂商 QUICKBOOT_POWERON、MY_PACKAGE_REPLACED；受用户自启开关和悬浮窗权限约束。接收阶段以前台 connectedDevice 类型启动，用户实际播放声音时才加入 mediaPlayback 类型，避免新系统禁止开机广播启动 mediaPlayback 的限制。服务被系统回收时允许 sticky 重建；用户主动停止则停止。网络地址就绪/变化后重播 mDNS，避免开机时 Wi-Fi 尚未就绪导致不可发现。

Android 10 的焦点机制需要原播放器配合；此前当前小米电视的 HDMI 组合已由用户现场确认声音可来回切换，混音结果以 VALIDATION_V3.md 为准。其他 HDMI 源可能有厂商音频路径，因此“焦点获批”和“HDMI 原声实际停下”必须分开验证。依据 [Android 官方音频焦点说明](https://developer.android.com/media/optimize/audio-focus)，不能把框架状态当作现场听感证明。某些播放器可能暂停播放而不只静音，需逐一实测。

## 验证与复现

- 构建与 lint 命令沿用 FLOATING.md 的工具链；源码不增加桌面 Web 构建依赖，网页直接作为 APK assets 打包。
- `tools/web_smoke.cjs`：Chrome / WebKit 的浏览器操作测试，使用独立的 fixture API，不代表真实 TV 音频已验证。安装 Playwright 到忽略的 `.tools/webqa` 后，设置 `PLAYWRIGHT_MODULE=../.tools/webqa/node_modules/playwright`；Chromium 复用本机独立启动的 Chrome，WebKit 通过 Playwright 安装。
- `tools/remote_smoke.py --url http://<电视IP>:8765`：真实电视 HTTP 校验，校验免配对、跨站拒绝、参数、移动/缩放、隐藏状态、投屏增益与空闲音频不抢焦点，并恢复窗口/音量设置。不要在家人正在投屏时运行布局测试。
- 当前 0.3.0 见 [VALIDATION_V3.md](VALIDATION_V3.md)；历史 0.2.0 现场记录见 [VALIDATION_V2.md](VALIDATION_V2.md)，0.2.1 离线构建记录见 [VALIDATION_V2_1.md](VALIDATION_V2_1.md)；第一版记录保留在 VALIDATION.md。设备/浏览器证据保存在忽略的 artifacts 中，不提交。

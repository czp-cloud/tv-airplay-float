# 第二版：手机网页遥控器

版本 `0.2.0-web-remote`，versionCode 37。HTTP 服务随电视 APK 运行，端口 8765；不需要电脑代理网页、云服务或外网资源。遥控的是本应用的小窗，不控制其他 App 或 HDMI 盒子的遥控按键。

## 手机使用

1. 在电视打开 AirPlay 投屏小窗并开启接收。控制页显示局域网地址与六位配对码。
2. 手机连接电视所在局域网，打开 `http://<电视IP>:8765`；输入配对码进入遥控器。接收器重启后会重新生成配对码，需要重新配对。
3. 方向按钮点按微调、长按连续移动；也可拖动中央摇杆斜向移动。松手、触摸取消、页面失焦或切到后台后停止连续移动。
4. 滑条按电视屏幕宽度显示百分比，调整的是绝对像素宽度；保持 16:9 小窗外框和源视频比例。可一键恢复右上角 320×180 小窗。
5. 声音开关左侧为电视原声，右侧为投屏声音，支持点选两侧和左右滑动。默认左侧，断开镜像后自动回到左侧；没有镜像/音频格式时不申请焦点或打开输出。退出网页不停止电视接收。

页面手机优先：320/375/390/430 像素竖屏单列，平板/电脑自动双列；大触控区、原生滑条、减少动态效果偏好、键盘操作、配对错误与断线重连提示。预览是位置示意，不采集或传输电视截图。

## 实现

- `RemoteControlServer.kt`：复用 [NanoHTTPD 2.3.1](https://github.com/NanoHttpd/nanohttpd)，四线程有界并发、5 秒 socket 超时；小型 JSON 请求、固定资源白名单，窗口和音频操作由 Handler 派发至主线程，2 秒超时的未执行任务会取消。
- 网页通过同源 HTTP 接口控制：`POST /api/pair {code}` 取得当前服务周期内的随机 192-bit token；后续请求使用 Authorization Bearer。配对码不包含在静态网页或状态 API 内；只在电视控制页和 DUMP 权限诊断里出现。配对尝试限制为每 30 秒 8 次，校验局域网来源、本机数字 IP Host 和 Origin，不启用 CORS，状态不缓存，不允许 iframe 嵌入。
- `GET /api/state`：接收、镜像、窗口与音频状态。`POST /api/move {dx,dy}`、`/api/size {width}`、`/api/reset {}`、`/api/audio {source:"tv"|"mirror"}`。未知参数、浮点移动、越界输入、错误方法会拒绝。没有 shell、任意文件读取或对其他 App 的控制接口。
- `MirrorOverlay.kt`：新增绝对宽度和屏幕/边界数据；空闲时调整持久设置，不添加可见窗口，保留第一版真实视频后显示、断开隐藏。
- `MirrorAudioController.kt`：仅在真实镜像会话且音频格式已到达时，申请临时独占音频焦点，再启动已有 AAC/ALAC → TimelineBuffer → Oboe 音频链；左侧、断开、永久焦点丢失或服务销毁时关闭输出并放弃焦点。短暂丢失焦点暂停本地声音，得到焦点后恢复。失败时退回电视原声；不通过全局静音伪造源切换。
- Oboe 使用 MEDIA usage，与音频焦点属性一致。第一版没有开启的音频输出按用户选择启用；系统媒体音量与发送端音量同步仍不改动。

Android 10 的焦点机制需要原播放器配合；HDMI 源可能有厂商音频路径，因此“焦点获批”和“HDMI 原声实际停下”必须分开验证。依据 [Android 官方音频焦点说明](https://developer.android.com/media/optimize/audio-focus)，不能把框架状态当作现场听感证明。某些播放器可能暂停播放而不只静音，需逐一实测。

## 验证与复现

- 构建与 lint 命令沿用 FLOATING.md 的工具链；源码不增加桌面 Web 构建依赖，网页直接作为 APK assets 打包。
- `tools/web_smoke.cjs`：Chrome / WebKit 的浏览器操作测试，使用独立的 fixture API，不代表真实 TV 音频已验证。安装 Playwright 到忽略的 `.tools/webqa` 后，设置 `PLAYWRIGHT_MODULE=../.tools/webqa/node_modules/playwright`；Chromium 复用本机独立启动的 Chrome，WebKit 通过 Playwright 安装。
- `tools/remote_smoke.py --url http://<电视IP>:8765`：真实电视 HTTP 校验，配对码通过 `TV_REMOTE_CODE` 环境变量传入；校验认证、跨站拒绝、参数、移动/缩放、隐藏状态、空闲音频不抢焦点并恢复窗口设置。不要在家人正在投屏时运行布局测试。
- 新版实际验证记录见 [VALIDATION_V2.md](VALIDATION_V2.md)；第一版记录保留在 VALIDATION.md。HTTP token、配对码和设备/浏览器证据保存在忽略的 artifacts 中，不提交。

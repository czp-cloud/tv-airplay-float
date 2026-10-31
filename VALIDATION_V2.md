# 第二版验证记录

日期：2026-10-31，Asia/Shanghai。目标沿用第一版 Xiaomi MiTV-MTEQ0 / baymax，Android 10 / API 29，ARM32。第一版现场验证保留在 VALIDATION.md。

## 构建与安装

- 版本 `0.2.0-web-remote`，versionCode 37，普通应用包名不变：`dev.czp.tvairplay`。
- `:app:assembleDebug :app:lintDebug -PtargetAbi=armeabi-v7a -PuseMavenMirror=true` 通过；Lint 0 errors、64 warnings、1 hint，未新增错误 baseline。
- APK 签名验证通过，29,868,473 字节，SHA-256 `97c740e4edb11964e65afadc2140475cccf11e6f7346cc0cb235166d89ca8cee`。
- 分块上传 609 块并验证 MD5，安装前核对远端 SHA-256，`pm install -r` 成功。
- 16:59–17:00 在无镜像会话时后台更新，未打开控制页、切换 HDMI 输入或取得窗口焦点。`artifacts/v2-device-smoke/report.json` 中 11 个断言通过，含空闲隐藏、100% 不透明、无标题外框、前台焦点与媒体音量不变。

## 浏览器与 HTTP

`tools/web_smoke.cjs` 使用隔离的 fixture API，Chrome 与 WebKit 各 23 项检查，共 46 项通过：

- 配对成功、错误配对保持锁定、退出回配对页。
- 320/375/390/430/768/1280 像素无横向溢出，实际截图检查手机单列、电脑双列。
- 方向长按重复移动，松手及 pointercancel 停止；真实触控点按有效。
- 中央摇杆可斜向移动、松手停止；滑条传绝对尺寸；声音左右选择；恢复角落。
- 接收状态同步、断线禁用操作、网络恢复后自动连接，无未捕获 JS 错误。

这些浏览器用例证明交互，不证明 TV 声音或 HDMI 已切换。修复了连接 toast 拦截摇杆触摸的问题，并重新跑通用例。截图与报告在忽略目录 `artifacts/web-smoke/`。

`tools/remote_smoke.py` 对电视 `:8765` 的真实服务运行，24 项通过：静态页面/防嵌入响应头、未配对拒绝状态与控制、跨 Origin / Host 拒绝、配对错误和成功、无 secret 状态泄露、未知/非法输入拒绝、方法及路径限制、绝对缩放和方向移动、空闲不显示窗口、恢复角落、无会话时音频不抢焦点、切回电视不保留输出/焦点、配对尝试限流。测试后恢复窗口设置与电视原声。报告 `artifacts/v2-http-smoke.json`。

隔离 Chrome 手机视口还实际打开电视网页、配对、调整至 600 像素再恢复右上角，未出现 JS 错误。证据 `artifacts/v2-browser-live.log` 与 `artifacts/v2-live-phone.png`。

## 音频与现场操作

当前通过的是空闲状态下的源选择、不打开音频输出、不抢焦点，以及电视原声默认值。真实镜像时焦点获批、AAC/ALAC 解码、Oboe 输出、HDMI 声音是否停止/恢复及画面是否继续，须记录运行时与用户现场听感。目前等待用户操作，不能把 fixture 浏览器或空闲 API 检查当作音频切换成功。

代码走 Android AudioFocusRequest 与现有原生音频链，不使用 root、全局静音、音量归零、ADB 注入按键或其他 App 的私有接口。Android 10 原播放器是否服从音频焦点需实测。

## 证据边界

- 未测长时间运行、连续滑动负载下的视频性能、各型号 TV/HDMI 路由、待机恢复、所有投屏 App、通话回声或 DRM。
- 配对码和 token 只在设备/忽略的本地证据中保存，不提交到 Git；此记录不包含任何配对凭证。
- 设备截图、日志、构建下载缓存与签名文件均不进入 Git。新增 NanoHTTPD 许可保存在 THIRD_PARTY_NOTICES.md。

# 0.3.1：横竖屏窗口比例自动跟随

验证日期：2026-11-01，Asia/Shanghai。设备 MiTV-MTEQ0 / Android 10，普通应用包名 dev.czp.tvairplay，无 root。

## 修改与规则

- 原先外框固定 16:9；现在利用已有 onVideoSize 回调中的视频宽高更新窗口比例，解码和视频内容保持原样。不能据此去除发送端已编码在视频里的黑边。
- 使用持久保存的最长边作为大小偏好；旧版宽度迁移为该值。旋转时不累计缩放，宽高均限制到电视相应尺寸的 80%，原位置只有越界时才调整。竖屏暂时受限不覆盖偏好，转回横屏可以恢复原先大小。
- 网页滑条发送 longEdge，预览按真实比例绘制并同步边界；旧 API 单字段 width 继续表示宽度。空闲不创建可见窗口，保留上一次比例用于预览。
- 音频处理和开机自启代码没有改动；用户选择保留自启、不重启电视的要求继续有效。

## 构建与自动检查

- assembleDebug、lintDebug、testDebugUnitTest 成功。Lint 0 errors、66 warnings、1 note。
- 7 项 JVM 几何测试通过：真实竖屏比例、旋转保留最长边、超大竖屏适配及横屏恢复、各种比例边界、小屏幕、兼容旧 width、无效比例回退。
- Chrome / WebKit 共 76 项浏览器检查通过，含手机排版、模拟横竖屏状态切换、预览比例、滑条和边界、此前摇杆/三种声音/音量/自启/断线恢复。手机截图已检查；浏览器使用 fixture API，不能代替真实 iPhone 旋转验收。证据 artifacts/web-auto-aspect/。
- 电视实际 HTTP 41 项检查通过，增加 longEdge 参数拒绝、禁止歧义双参数、最长边尺寸和比例检查；测试时无投屏，结束后恢复原位置、最长边 1306、投屏音量 42% 和开启自启。证据 artifacts/auto-aspect-http.json。

## 安装与运行

- 版本 0.3.1-auto-aspect，versionCode 40，ARM32；APK 29,777,997 字节，签名 v2 校验通过。
- SHA-256：f52dc61f8563d7ba441c38d53fa70847be984daed0191107b50334555b57cc08。忽略的 artifacts/releases/tv-airplay-float-0.3.1-debug.apk 保存对应文件。
- 用户确认已停止投屏；安装前再次核对型号和无镜像/连接，上传 MD5 与远端 SHA-256 校验通过，后台 pm install -r 成功。
- 安装 9 项检查通过：自动恢复接收服务、空闲隐藏窗口、API 版本、自启开启、电视前台保持不变等；未打开全屏控制页，未重启电视。证据 artifacts/auto-aspect-install/。

## 现场验收边界

已向用户请求重新连接 iPhone，在竖屏 → 横屏 → 竖屏间切换并试用大小滑条。当前等待用户确认；实际画面、旋转持续输出与主观观感不以 fixture 测试冒充通过。只读状态观察记录在忽略的 artifacts/auto-aspect-live/。

混音已通过的历史现场结果见 VALIDATION_V3.md。本轮未重新进行有声混音试听，也未进行真正开机自启验证。

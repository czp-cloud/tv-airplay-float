# 0.3.3：手机遥控器透明度滑条

日期：2026-11-01，Asia/Shanghai。目标 MiTV-MTEQ0 / Android 10 / ARM32，包名 dev.czp.tvairplay。

## 功能

- 在窗口大小下方增加“窗口透明度”原生滑条，范围 12–100%，数值越低越透明；100% 完全不透明。沿用原先悬浮窗最低 alpha 0.12 的范围。
- 拖动实时发送并合并待执行的 opacity 命令，位置预览同步透明度，显示当前百分比；支持触摸、键盘、刷新与断线恢复。
- 新增 POST /api/opacity，唯一参数为整数 opacity（12..100），映射到已有 MirrorOverlay.opacity → WindowManager.LayoutParams.alpha 与持久化逻辑。空闲时可预设，不添加窗口；默认设置不变。
- “恢复右上角”沿用原行为，同时恢复 alpha=1；取消待执行的旧透明度命令。横竖屏、贴边和音频策略未改动。

## 构建与浏览器

- 0.3.3-opacity / versionCode 42。assembleDebug / lintDebug 成功，Lint 0 errors、66 warnings、1 note。未改几何计算，本轮不重复几何单测。
- Chrome / WebKit 共 90 项 fixture 检查通过，包括透明度控制在 TV 声音模式下可用、55% 映射及预览、刷新保留、键盘选择 12% 和 100%、恢复完全不透明，以及已有手机排版、横竖屏预览、摇杆、声音与自启检查。已检查手机截图。证据 artifacts/web-opacity/。
- APK 29,778,337 字节，签名 v2 通过；SHA-256 ae978c5e1260e46aa9b29fa1803670c447b10ecdfb329275dd3dbccb04472cd3。对应文件保存在忽略的 artifacts/releases/tv-airplay-float-0.3.3-debug.apk。

## 电视验证

- 安装前再次核对 MiTV-MTEQ0、无镜像与连接，上传 MD5 与远端 SHA-256 一致，后台安装成功。9 项安装检查通过：安装后系统自动恢复接收、空闲隐藏、API 版本 0.3.3、自启保持开启、电视前台保持不变等。证据 artifacts/opacity-install/。
- 真实电视 HTTP 54 项检查通过，新增透明度 11/101/55.5 的拒绝、12/55/100 对 alpha 0.12/0.55/1 的映射与空闲不显示窗口、恢复小窗时完全不透明。证据 artifacts/opacity-http.json。
- 结束后恢复原位置 (1520,152)、最长边偏好 1306、竖屏显示 400×864、透明度 100%、投屏音量 42% 与开启自启。只读再次确认真实网页已包含 opacitySlider，服务正常、空闲无音频输出；未重启电视。
- 浏览器 fixture 与空闲 HTTP 不作为投屏画面透明度的现场观感证明；真实投屏时拖动滑条的最终视觉效果留待用户使用确认。复用的底层 alpha 路径此前已经在本电视的透明调试窗口中使用。

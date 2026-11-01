# 电视同时发声与左右声道：可行性研究

结论：0.3.0 的“电视原声 + 投屏声音同时播放”已在本台小米电视的 HDMI 输入场景通过用户试听，网页提供 tv / mirror / mixed 三选项和独立投屏音量。当前方案无需 root，也无需左右拆分。让投屏 PCM 只出右声道在本应用里可实现；但这不等于能把 HDMI 原声只放左声道，后一半需要控制不属于本应用的电视音频路径，左右分离仍未实现。

## 当前已知证据

这台 MiTV-MTEQ0 为 Android 10。0.2.0 已现场证实电视与投屏声音可来回切换：投屏使用普通 UID 10051 的 AAudio，44100 Hz / 双声道；取得临时独占音频焦点时 TV 播放器收到 LOSS_TRANSIENT，释放后恢复。证据在 VALIDATION_V2.md。

既有 dumpsys 中 STREAM_MUSIC 标记 SPDIF，而 Audio routes 的 mMainType=0x0。不能仅凭这个标签认定声音实际来自外置音箱，也不能据此认定电视左右物理单元能被单独寻址。后续 0.3.0 实机采集已得到 AudioPolicy / AudioFlinger / 厂商配置：内置 Speaker 为双声道 48kHz，Speaker 和 SPDIF 路由声明 type=mix，来源同时包含 primary output 与 HDMI In；配置与听感仍分开验收。最新设备证据见 VALIDATION_V3.md。

## 路线比较

| 路线 | 普通 APK 能控制的部分 | 还需要确认的条件 |
| --- | --- | --- |
| 两路同时发声 | 调整本应用的焦点策略与 PCM 增益，输出正常立体声 | TV/HDMI 硬件通路能否与软件 AAudio 混合；原播放器如何响应 MAY_DUCK |
| 投屏仅右声道 | 对自身解码 PCM 做左右声道处理 | 物理输出是否保留立体声；电视音效/DSP 是否把两声道重新混合 |
| 左边仅 TV、右边仅投屏 | 自身右声道可做 | 还必须把 TV 原声从右声道去掉并将 TV 内容适当下混到左声道；现有 APK 没有该音频源的控制权 |
| 统一软件混音与分声道 | 若两路 PCM 都归本应用，可完全计算/混音 | 当前拿不到 HDMI 原声 PCM；不能把屏幕镜像成功理解为已有 HDMI 音频采集能力 |

## 同时发声的原理与验证

Android [音频焦点文档](https://developer.android.com/media/optimize/audio-focus)说明，同一输出可以混合多个音频流；Android 10 的焦点主要依赖应用协作。当前二选一是应用主动申请临时独占焦点的结果，不能据此认定硬件只能播放一路。

[AOSP TV 音频设计](https://source.android.com/docs/core/audio/tv)说明，TV 输入通过音频端口与 patch 路由，硬件最终输出可以与软件流混合。这给出可试的架构路线，并不证明小米这台具体固件已完整实现。

实际实现的 mixed 模式释放本应用的独占焦点，保留电视原声的播放条件，同时启动共享 AAudio 输出；不申请 MAY_DUCK、不伪装通知或通话、不改系统全局静音。投屏 PCM 使用本应用独立增益，音量滑条不会修改电视系统媒体音量。

2026-11-01，用户让 HDMI 盒子与 iPhone 镜像分别播放有声内容，选择 mixed 后反馈“两路都能听到，混合正常”。随后只读 API 显示 mixed、outputActive=true、focusHeld=false、投屏音量 18%；现场听感证实这条路径可以自然混合。完整边界见 VALIDATION_V3.md：未测长期稳定性，也不据此保证其他电视或数字直通/外接音响路径。

独立投屏音量可以把运动指导压低，同时保留电视节目的原音量。无需为了同时发声而拆分左右声道。

## 左右分离的边界

当前原生 `audio_output.h` 输出 PCM16 双声道，回调已能对本应用样本乘以独立音量增益。以后可以先把投屏左右样本平均为 mono，再将左样本置 0、右样本写入 mono×gain；这样不会只丢掉原素材的左边内容。这只是实现草案，未添加或实测。普通 [AudioTrack 的左右增益](https://developer.android.com/reference/android/media/AudioTrack#setStereoVolume(float,%20float))同样只影响该 track，不能控制其他 App 或 HDMI 输入。

TV 原声仍会在它自己的通路里播放，通常不会因为我们左声道置零就从右边消失。调整系统总平衡还可能同时影响两路，不能用来保证逐源分离。

已检查 Android 10.0.0_r1 的 [AudioPolicyService](https://android.googlesource.com/platform/frameworks/av/+/refs/tags/android-10.0.0_r1/services/audiopolicy/service/AudioPolicyInterfaceImpl.cpp)：createAudioPatch、releaseAudioPatch、setAudioPortConfig 均有 modifyAudioRoutingAllowed 权限检查。Java AudioManager 中这些接口也是隐藏/系统接口。现有普通 APK 不能把“ADB 已授权”当作拥有这些系统音频权限；有系统权限也仍需确认厂商端口是否支持逐声道增益。

另一种设想是捕获 TV 声音后在本应用内重新混音，但 [AudioPlaybackCapture](https://developer.android.com/media/platform/av-capture)要求用户授权与被采集端允许。旧 target SDK 的播放器默认不开放，HDMI 硬件 patch 也不能保证成为普通 App 可采集的播放流。当前不加麦克风权限、不录制家庭声音，也不承诺通过 root 就一定能解决。

## 只读采集入口

0.2.1 APK 内新增 DUMP 权限保护的 `audio-devices` 诊断，仅枚举输出设备的 ID/type/channelCounts/channelMasks/sampleRates/address。主机工具 `tools/inspect_tv_audio.py` 核对型号后采集 dumpsys audio、media.audio_policy、media.audio_flinger、tv_input 以及已有配置文件。所有输出保存在忽略的 artifacts，不设置路由、不切音、不调音量、不申请 root。

电视在线且安装新版后：

```sh
python3 tools/inspect_tv_audio.py --host <电视IP> --expected-model MiTV-MTEQ0
```

当前自然混音已在本台电视的 HDMI 场景通过用户试听。左右隔离仍是研究方向，未展示为产品功能。

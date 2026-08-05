# LSPosed

一个用于承载多个个人功能区的 LSPosed 总模块。当前包含“电源键语音”和“短信拦截推送”：前者把魅族 / Flyme 的长按电源键映射到国内 AI 应用语音通话，后者捕获系统收到的短信并转发到用户配置的飞书机器人。

Flyme 的长按电源键逻辑会走系统私有入口，常规的“默认语音助手”设置并不能稳定拉起第三方语音通话。电源键语音功能会在系统侧拦截长按电源键，在目标 AI 应用侧学习并复用应用自己生成的语音通话 Intent，从而避免硬编码各厂商内部路由参数。

## 功能区

| 功能区 | 状态 | 说明 |
| --- | --- | --- |
| 电源键语音 | 已实现 | 长按魅族电源键进入已学习的 AI 语音通话 |
| 短信拦截推送 | 已实现 | 转发完整短信；识别到验证码时在飞书卡片标题中醒目展示 |

## 电源键语音

- 长按电源键直接进入已学习的 AI 语音通话。
- 可在模块首页选择目标 AI 应用；未选择时默认优先豆包。
- 通过 Provider 抽象适配多个国内 AI 应用，语音通话入口按应用独立学习和保存。
- 不修改系统分区，不改目标应用 APK，禁用模块后即可恢复。

### 内置 Provider

| 应用 | 包名 |
| --- | --- |
| 豆包 | `com.larus.nova` |
| Kimi | `com.moonshot.kimichat` |
| 通义千问 | `com.aliyun.tongyi` |
| 文心一言 / 文小言 | `com.baidu.newapp` |
| 讯飞星火 | `com.iflytek.spark` |
| 腾讯元宝 | `com.tencent.hunyuan.app.chat` |
| 智谱清言 | `com.zhipuai.qingyan` |
| DeepSeek | `com.deepseek.chat` |
| MiniMax / 海螺 AI | `com.xproducer.yingshiai` |

## 环境要求

- 魅族 / Flyme 设备。
- 已 Root，并安装 Magisk / Zygisk + LSPosed。
- 已安装至少一个内置 Provider 对应的 AI 应用。
- 建议先确认长按电源键在系统日志中会进入 Flyme 的 `powerLongPress` 链路。

## 短信拦截推送

1. 在模块首页进入“通知服务”，添加自己的飞书机器人 Webhook。
2. 进入“短信拦截推送”，勾选要使用的机器人。
3. 在 LSPosed 作用域中勾选 `com.android.phone`，然后软重启或重启手机。
4. 普通短信会完整转发；识别到验证码时，卡片标题显示具体验证码并使用红色标题栏。

项目不会在源码或 APK 中内置任何飞书 Webhook，新安装后需要用户自行配置。

## 安装使用

1. 安装 APK。
2. 在 LSPosed 中启用 `LSPosed`。
3. 作用域勾选 `android` 和目标 AI 应用。
4. 重启手机或在 LSPosed 里软重启。
5. 打开模块首页，选择目标 AI 应用。
6. 手动打开目标 AI 应用，点一次“语音通话”，让模块学习当前版本的入口。
7. 之后长按电源键，应直接进入该应用的语音通话。

包名为：

```text
io.github.ultralan.lsposed
```

这是从旧的单功能模块改成总模块后的新包名。Android 会把新版当成另一个应用，请在 LSPosed 里重新勾选新版模块和作用域；旧模块建议禁用或卸载。

## 构建

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew clean :app:testDebugUnitTest :app:assembleDebug
```

产物位置：

```text
app/build/outputs/apk/debug/LSPosed.apk
```

推送 `v*` 标签后，GitHub Actions 会使用仓库 Secrets 中的固定签名构建正式 APK，并发布到 GitHub Releases。

## 调试

先清日志，操作一次长按电源键，再导出关键日志：

```bash
adb logcat -c
adb logcat -d -v time | grep -Ei 'LSPosed|MzPhoneWindowManager|powerLongPress|voice|call|com.larus.nova|com.moonshot.kimichat|com.aliyun.tongyi|com.baidu.newapp|com.iflytek.spark|com.tencent.hunyuan.app.chat|com.zhipuai.qingyan|com.deepseek.chat|com.xproducer.yingshiai'
```

Windows PowerShell 可用：

```powershell
adb logcat -d -v time | findstr /i "LSPosed MzPhoneWindowManager powerLongPress voice call com.larus.nova com.moonshot.kimichat com.aliyun.tongyi com.baidu.newapp com.iflytek.spark com.tencent.hunyuan.app.chat com.zhipuai.qingyan com.deepseek.chat com.xproducer.yingshiai"
```

更多说明见 [docs/usage.md](docs/usage.md) 和 [docs/troubleshooting.md](docs/troubleshooting.md)。

## 已知限制

- 各厂商内部语音通话入口可能随版本变化，需要先手动点一次语音通话完成学习。
- 如果目标 AI 应用没有在模块首页选中，系统侧会按豆包优先的默认策略启动。
- 非豆包应用首版主要依赖通用语音/通话 Intent 特征识别；如果某个应用无法学习，需要用日志补充 Provider 专属规则。
- 不保证支持从系统外部直接启动各应用内部通话 Activity，因为很多 Activity 不导出。

## License

MIT License

# 使用指南

## 首次配置

1. 安装 `LSPosed` APK。
2. 打开 LSPosed，启用本模块。
3. 进入模块作用域，勾选：
   - `android`
   - 目标 AI 应用，例如 `豆包 / com.larus.nova`
4. 重启手机或在 LSPosed 中执行软重启。
5. 打开模块首页，在“电源键语音”功能区选择目标 AI 应用。
6. 手动打开目标 AI 应用并点一次“语音通话”。
7. 长按电源键测试是否进入该应用的语音通话。

第 6 步是必要步骤。语音通话的真实启动参数由各 AI 应用自己生成，模块会在目标应用进程内学习这次启动参数，之后再由电源键触发复用。

未选择目标时默认优先豆包。如果选中的应用未安装，系统侧会按内置 Provider 顺序选择第一个已安装应用。

## 功能区状态

- 电源键语音：已实现。
- 短信拦截推送：已实现。需要在“通知服务”中添加自己的飞书机器人，并在 LSPosed 作用域中勾选 `com.android.phone`。
- 应用更新：已实现。首页可检查 GitHub Releases、下载并校验正式 APK，然后进入系统安装确认。

首次安装已经配置公开默认飞书机器人，短信模块会默认选择该通道；可以在“通知服务”中停用、删除或新增其他机器人。

## 内置 Provider

- 豆包：`com.larus.nova`
- Kimi：`com.moonshot.kimichat`
- 通义千问：`com.aliyun.tongyi`
- 文心一言 / 文小言：`com.baidu.newapp`
- 讯飞星火：`com.iflytek.spark`
- 腾讯元宝：`com.tencent.hunyuan.app.chat`
- 智谱清言：`com.zhipuai.qingyan`
- DeepSeek：`com.deepseek.chat`
- MiniMax / 海螺 AI：`com.xproducer.yingshiai`

## ADB 验证

完成首次学习后，可以用下面命令验证豆包侧触发链路：

```bash
adb shell am start \
  -n com.larus.nova/com.larus.home.impl.alias.AliasActivity3 \
  --ez io.github.ultralan.lsposed.powervoice.TRIGGER_VOICE_CALL true \
  --es io.github.ultralan.lsposed.powervoice.TRIGGER_PROVIDER_ID doubao \
  --es io.github.ultralan.lsposed.powervoice.TRIGGER_SOURCE adb_test
```

预期结果：

```text
豆包打开，并进入有语音输出的语音通话界面。
日志中出现：复用已学习 Intent 启动豆包语音通话。
```

## 恢复默认行为

在 LSPosed 禁用本模块并重启。模块不会修改系统分区，也不会修改任何 AI 应用 APK。

## 旧版本迁移

总模块包名为 `io.github.ultralan.lsposed`。如果你安装过旧的单功能包名版本，请在 LSPosed 里禁用旧模块，并给新版重新勾选作用域。

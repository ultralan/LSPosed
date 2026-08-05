# 排障指南

## 抓取日志

```bash
adb logcat -c
```

长按一次电源键后：

```bash
adb logcat -d -v time | grep -Ei 'LSPosed|MzPhoneWindowManager|powerLongPress|voice|call|com.larus.nova|com.moonshot.kimichat|com.aliyun.tongyi|com.baidu.newapp|com.iflytek.spark|com.tencent.hunyuan.app.chat|com.zhipuai.qingyan|com.deepseek.chat|com.xproducer.yingshiai'
```

Windows PowerShell：

```powershell
adb logcat -d -v time | findstr /i "LSPosed MzPhoneWindowManager powerLongPress voice call com.larus.nova com.moonshot.kimichat com.aliyun.tongyi com.baidu.newapp com.iflytek.spark com.tencent.hunyuan.app.chat com.zhipuai.qingyan com.deepseek.chat com.xproducer.yingshiai"
```

## 长按电源键没有反应

先确认 LSPosed 作用域同时勾选了 `android` 和目标 AI 应用包名，然后重启手机或软重启。

再打开模块首页，确认“当前目标”显示的是你要使用的 AI 应用。

日志里应该能看到类似内容：

```text
系统侧已 hook
系统侧准备启动 <应用名>
```

如果没有这些日志，通常是 `android` 作用域未生效或没有重启。

## 只打开应用首页

这通常表示模块还没有学到该应用的语音通话入口。手动打开目标 AI 应用，点一次“语音通话”，等正常通话界面出现后再测试长按电源键。

学习成功后日志里应该出现：

```text
已学习 <应用名> 语音通话 Intent
```

## 进入通话但没有语音输出

这通常表示缓存的 Intent 参数已经不适配当前应用版本。重新手动点一次“语音通话”，让模块刷新学习结果。

## 应用升级后失效

AI 应用升级可能改变内部路由参数。先重新手动点一次“语音通话”；如果仍失败，再抓取日志排查。

## 某个非豆包应用无法学习

首版对非豆包应用使用通用语音/通话 Intent 特征识别。如果目标应用的内部 Activity、action、data 中完全不带 voice/call/rtc/audio/speech/语音/通话等特征，模块可能无法判断这次启动就是语音通话。抓取日志后，需要给该 Provider 补一条专属匹配规则。

## 总是启动豆包

打开模块首页，选择目标 AI 应用，并确认 LSPosed 作用域勾选了该应用。未选择目标时，默认策略会优先启动豆包。

## 短信拦截推送不可用

短信拦截推送目前只是预留功能区。本版本不申请短信权限，也不会安装任何短信相关 Hook。

## 卸载或禁用

在 LSPosed 禁用模块并重启即可恢复 Flyme 默认长按电源键行为。由于总模块包名已经变更，旧包名版本需要单独在系统应用管理或 LSPosed 中处理。

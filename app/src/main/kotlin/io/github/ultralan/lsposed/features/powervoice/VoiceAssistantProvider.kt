package io.github.ultralan.lsposed.features.powervoice

data class VoiceAssistantProvider(
    val id: String,
    val displayName: String,
    val packageName: String,
    val launcherActivity: String? = null,
    val exactVoiceCallActivities: Set<String> = emptySet(),
    val voiceCallKeywords: Set<String> = DEFAULT_VOICE_CALL_KEYWORDS,
) {
    companion object {
        val DEFAULT_VOICE_CALL_KEYWORDS = setOf(
            "voice",
            "call",
            "realtime",
            "rtc",
            "audio",
            "speech",
            "phone",
            "voip",
            "语音",
            "通话",
            "电话",
            "实时",
            "对话",
        )
    }
}

object VoiceAssistantProviders {
    val ALL: List<VoiceAssistantProvider> = listOf(
        VoiceAssistantProvider(
            id = "doubao",
            displayName = "豆包",
            packageName = PowerVoiceConfig.DOUBAO_PACKAGE,
            launcherActivity = PowerVoiceConfig.DOUBAO_LAUNCHER_ACTIVITY,
            exactVoiceCallActivities = setOf(PowerVoiceConfig.DOUBAO_REALTIME_CALL_ACTIVITY),
        ),
        VoiceAssistantProvider(
            id = "kimi",
            displayName = "Kimi",
            packageName = "com.moonshot.kimichat",
        ),
        VoiceAssistantProvider(
            id = "tongyi",
            displayName = "通义千问",
            packageName = "com.aliyun.tongyi",
        ),
        VoiceAssistantProvider(
            id = "wenxin",
            displayName = "文心一言/文小言",
            packageName = "com.baidu.newapp",
        ),
        VoiceAssistantProvider(
            id = "spark",
            displayName = "讯飞星火",
            packageName = "com.iflytek.spark",
        ),
        VoiceAssistantProvider(
            id = "yuanbao",
            displayName = "腾讯元宝",
            packageName = "com.tencent.hunyuan.app.chat",
        ),
        VoiceAssistantProvider(
            id = "zhipu",
            displayName = "智谱清言",
            packageName = "com.zhipuai.qingyan",
        ),
        VoiceAssistantProvider(
            id = "deepseek",
            displayName = "DeepSeek",
            packageName = "com.deepseek.chat",
        ),
        VoiceAssistantProvider(
            id = "minimax",
            displayName = "MiniMax/海螺 AI",
            packageName = "com.xproducer.yingshiai",
        ),
    )

    val defaultProvider: VoiceAssistantProvider = ALL.first()

    private val byId = ALL.associateBy { it.id }
    private val byPackage = ALL.associateBy { it.packageName }

    fun byId(id: String): VoiceAssistantProvider? = byId[id]

    fun byPackage(packageName: String): VoiceAssistantProvider? = byPackage[packageName]
}

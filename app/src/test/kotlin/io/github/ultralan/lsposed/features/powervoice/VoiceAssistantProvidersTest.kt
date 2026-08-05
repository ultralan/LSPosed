package io.github.ultralan.lsposed.features.powervoice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VoiceAssistantProvidersTest {
    @Test
    fun `built in providers include supported domestic AI apps`() {
        val providers = VoiceAssistantProviders.ALL.associateBy { it.id }

        assertEquals("com.larus.nova", providers.getValue("doubao").packageName)
        assertEquals("com.moonshot.kimichat", providers.getValue("kimi").packageName)
        assertEquals("com.aliyun.tongyi", providers.getValue("tongyi").packageName)
        assertEquals("com.baidu.newapp", providers.getValue("wenxin").packageName)
        assertEquals("com.iflytek.spark", providers.getValue("spark").packageName)
        assertEquals("com.tencent.hunyuan.app.chat", providers.getValue("yuanbao").packageName)
        assertEquals("com.zhipuai.qingyan", providers.getValue("zhipu").packageName)
        assertEquals("com.deepseek.chat", providers.getValue("deepseek").packageName)
        assertEquals("com.xproducer.yingshiai", providers.getValue("minimax").packageName)
    }

    @Test
    fun `providers can be resolved by package name and id`() {
        assertEquals("豆包", VoiceAssistantProviders.byId("doubao")?.displayName)
        assertEquals("Kimi", VoiceAssistantProviders.byPackage("com.moonshot.kimichat")?.displayName)
        assertNotNull(VoiceAssistantProviders.defaultProvider)
    }
}

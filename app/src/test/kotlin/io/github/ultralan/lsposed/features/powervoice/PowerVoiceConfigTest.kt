package io.github.ultralan.lsposed.features.powervoice

import kotlin.test.Test
import kotlin.test.assertEquals

class PowerVoiceConfigTest {
    @Test
    fun `doubao component names are stable`() {
        assertEquals("com.larus.nova", PowerVoiceConfig.DOUBAO_PACKAGE)
        assertEquals("com.larus.home.impl.alias.AliasActivity3", PowerVoiceConfig.DOUBAO_LAUNCHER_ACTIVITY)
        assertEquals("com.larus.voicecall.impl.ui.RealtimeCallActivity", PowerVoiceConfig.DOUBAO_REALTIME_CALL_ACTIVITY)
    }

    @Test
    fun `trigger extra uses project namespace`() {
        assertEquals(
            "io.github.ultralan.lsposed.powervoice.TRIGGER_DOUBAO_VOICE_CALL",
            PowerVoiceConfig.EXTRA_TRIGGER_DOUBAO_VOICE_CALL,
        )
    }
}

package io.github.ultralan.lsposed.features.powervoice

import android.content.ComponentName
import android.content.Intent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class VoiceCallIntentMatcherTest {
    @Test
    fun `doubao exact realtime call activity matches voice call`() {
        val provider = VoiceAssistantProviders.byId("doubao")!!
        val intent = Intent().setComponent(
            ComponentName(provider.packageName, "com.larus.voicecall.impl.ui.RealtimeCallActivity"),
        )

        assertTrue(VoiceCallIntentMatcher.matches(provider, intent))
    }

    @Test
    fun `keyword activity in provider package matches voice call`() {
        val provider = VoiceAssistantProviders.byId("kimi")!!
        val intent = Intent().setComponent(
            ComponentName(provider.packageName, "com.moonshot.kimichat.voice.VoiceCallActivity"),
        )

        assertTrue(VoiceCallIntentMatcher.matches(provider, intent))
    }

    @Test
    fun `keyword activity from another package does not match provider`() {
        val provider = VoiceAssistantProviders.byId("kimi")!!
        val intent = Intent().setComponent(
            ComponentName("com.example.other", "com.example.voice.VoiceCallActivity"),
        )

        assertFalse(VoiceCallIntentMatcher.matches(provider, intent))
    }

    @Test
    fun `non voice activity in provider package is ignored`() {
        val provider = VoiceAssistantProviders.byId("kimi")!!
        val intent = Intent().setComponent(
            ComponentName(provider.packageName, "com.moonshot.kimichat.home.MainActivity"),
        )

        assertFalse(VoiceCallIntentMatcher.matches(provider, intent))
    }
}

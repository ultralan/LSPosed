package io.github.ultralan.lsposed.features.powervoice

import android.content.ComponentName
import android.content.Intent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class IntentDiagnosticsTest {
    @Test
    fun `summary logs intent shape without extra values`() {
        val provider = VoiceAssistantProviders.byId("tongyi")!!
        val intent = Intent(Intent.ACTION_VIEW)
            .setComponent(ComponentName(provider.packageName, "com.aliyun.tongyi.home.MainActivity"))
            .setData(android.net.Uri.parse("tongyi://home"))
            .putExtra("conversation_id", "secret-conversation")
            .putExtra("source", "voice-button")

        val summary = IntentDiagnostics.summary(provider, intent)

        assertContains(summary, "provider=通义千问")
        assertContains(summary, "matched=false")
        assertContains(summary, "reason=provider package matched but no voice-call signal")
        assertContains(summary, "component=com.aliyun.tongyi/com.aliyun.tongyi.home.MainActivity")
        assertContains(summary, "data=tongyi://home")
        assertContains(summary, "extrasKeys=conversation_id,source")
        assertFalse(summary.contains("secret-conversation"))
        assertFalse(summary.contains("voice-button"))
    }

    @Test
    fun `summary explains non provider package`() {
        val provider = VoiceAssistantProviders.byId("tongyi")!!
        val intent = Intent().setComponent(
            ComponentName("com.example.other", "com.example.other.VoiceCallActivity"),
        )

        val summary = IntentDiagnostics.summary(provider, intent)

        assertContains(summary, "matched=false")
        assertContains(summary, "reason=component package com.example.other != com.aliyun.tongyi")
    }
}

package io.github.ultralan.lsposed.features.powervoice

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class IntentStoreTest {
    @Test
    fun `encode and decode keeps component data and extras`() {
        val source = Intent()
            .setComponent(ComponentName(PowerVoiceConfig.DOUBAO_PACKAGE, PowerVoiceConfig.DOUBAO_REALTIME_CALL_ACTIVITY))
            .setData(Uri.parse("doubao://flow/test"))
            .putExtra("sample_key", "sample_value")

        val decoded = IntentStore.decode(IntentStore.encode(source))

        assertNotNull(decoded)
        assertEquals(source.component, decoded.component)
        assertEquals("doubao://flow/test", decoded.dataString)
        assertEquals("sample_value", decoded.getStringExtra("sample_key"))
    }

    @Test
    fun `provider store keeps learned intents separated by provider`() {
        val context = RuntimeEnvironment.getApplication()
        val doubao = VoiceAssistantProviders.byId("doubao")!!
        val kimi = VoiceAssistantProviders.byId("kimi")!!
        val doubaoIntent = Intent().setComponent(
            ComponentName(doubao.packageName, "com.larus.voicecall.impl.ui.RealtimeCallActivity"),
        )
        val kimiIntent = Intent().setComponent(
            ComponentName(kimi.packageName, "com.moonshot.kimichat.voice.VoiceCallActivity"),
        )

        IntentStore.save(context, doubao, doubaoIntent)
        IntentStore.save(context, kimi, kimiIntent)

        assertEquals(doubaoIntent.component, IntentStore.load(context, doubao)?.component)
        assertEquals(kimiIntent.component, IntentStore.load(context, kimi)?.component)
    }
}

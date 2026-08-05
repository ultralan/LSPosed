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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class LearnedVoiceCallIntentStoreTest {
    @Test
    fun `publish and query learned voice call intent through content provider`() {
        val context = RuntimeEnvironment.getApplication()
        val provider = VoiceAssistantProviders.byId("doubao")!!
        val learnedIntent = Intent()
            .setComponent(ComponentName(provider.packageName, PowerVoiceConfig.DOUBAO_REALTIME_CALL_ACTIVITY))
            .setData(Uri.parse("doubao://voice-call/session"))
            .putExtra("sample_key", "sample_value")

        assertTrue(LearnedVoiceCallIntentStore.publish(context, provider, learnedIntent))

        val queried = LearnedVoiceCallIntentStore.query(context, provider)

        assertNotNull(queried)
        assertEquals(learnedIntent.component, queried.component)
        assertEquals("doubao://voice-call/session", queried.dataString)
        assertEquals("sample_value", queried.getStringExtra("sample_key"))
    }

    @Test
    fun `query or migrate local learned intent publishes legacy provider preference`() {
        val context = RuntimeEnvironment.getApplication()
        val provider = VoiceAssistantProviders.byId("doubao")!!
        val legacyIntent = Intent()
            .setComponent(ComponentName(provider.packageName, PowerVoiceConfig.DOUBAO_REALTIME_CALL_ACTIVITY))

        IntentStore.save(context, provider, legacyIntent)
        assertNull(LearnedVoiceCallIntentStore.query(context, provider))

        val resolved = LearnedVoiceCallIntentStore.queryOrMigrateLocal(context, provider)

        assertNotNull(resolved)
        assertEquals(legacyIntent.component, resolved.component)
        assertEquals(legacyIntent.component, LearnedVoiceCallIntentStore.query(context, provider)?.component)
    }
}

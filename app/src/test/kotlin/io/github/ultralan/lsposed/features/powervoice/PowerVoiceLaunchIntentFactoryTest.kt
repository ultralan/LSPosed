package io.github.ultralan.lsposed.features.powervoice

import android.content.ComponentName
import android.content.Intent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PowerVoiceLaunchIntentFactoryTest {
    @Test
    fun `learned voice call intent is launched before provider launcher`() {
        val context = RuntimeEnvironment.getApplication()
        val provider = VoiceAssistantProviders.byId("doubao")!!
        val learnedIntent = Intent()
            .setComponent(ComponentName(provider.packageName, PowerVoiceConfig.DOUBAO_REALTIME_CALL_ACTIVITY))

        LearnedVoiceCallIntentStore.publish(context, provider, learnedIntent)

        val launchIntent = PowerVoiceLaunchIntentFactory.create(context, provider)

        assertEquals(learnedIntent.component, launchIntent?.component)
        assertTrue(launchIntent?.flags?.and(Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertFalse(launchIntent?.getBooleanExtra(TriggerExtras.EXTRA_TRIGGER_VOICE_CALL, false) ?: true)
    }

    @Test
    fun `provider launcher is used as trigger fallback when no learned intent exists`() {
        val context = RuntimeEnvironment.getApplication()
        val provider = VoiceAssistantProviders.byId("doubao")!!

        val launchIntent = PowerVoiceLaunchIntentFactory.create(context, provider)

        assertEquals(ComponentName(provider.packageName, PowerVoiceConfig.DOUBAO_LAUNCHER_ACTIVITY), launchIntent?.component)
        assertTrue(launchIntent?.flags?.and(Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertTrue(launchIntent?.flags?.and(Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0)
        assertTrue(launchIntent?.getBooleanExtra(TriggerExtras.EXTRA_TRIGGER_VOICE_CALL, false) ?: false)
        assertEquals(provider.id, launchIntent?.getStringExtra(TriggerExtras.EXTRA_TRIGGER_PROVIDER_ID))
        assertEquals(
            TriggerExtras.TRIGGER_SOURCE_POWER_LONG_PRESS,
            launchIntent?.getStringExtra(TriggerExtras.EXTRA_TRIGGER_SOURCE),
        )
    }
}

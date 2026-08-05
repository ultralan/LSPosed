package io.github.ultralan.lsposed.features.powervoice

import android.content.ComponentName
import android.content.Context
import android.content.Intent

object PowerVoiceLaunchIntentFactory {
    fun create(context: Context, provider: VoiceAssistantProvider): Intent? {
        val learned = LearnedVoiceCallIntentStore.query(context, provider)
        if (learned != null) {
            return Intent(learned).apply {
                removeTriggerExtras()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return provider.launchIntent(context)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(TriggerExtras.EXTRA_TRIGGER_VOICE_CALL, true)
            putExtra(TriggerExtras.EXTRA_TRIGGER_PROVIDER_ID, provider.id)
            putExtra(TriggerExtras.EXTRA_TRIGGER_SOURCE, TriggerExtras.TRIGGER_SOURCE_POWER_LONG_PRESS)
        }
    }

    private fun VoiceAssistantProvider.launchIntent(context: Context): Intent? {
        if (launcherActivity != null) {
            return Intent().setComponent(ComponentName(packageName, launcherActivity))
        }
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }

    private fun Intent.removeTriggerExtras() {
        removeExtra(TriggerExtras.EXTRA_TRIGGER_VOICE_CALL)
        removeExtra(TriggerExtras.EXTRA_TRIGGER_PROVIDER_ID)
        removeExtra(TriggerExtras.EXTRA_TRIGGER_SOURCE)
        removeExtra(PowerVoiceConfig.EXTRA_TRIGGER_DOUBAO_VOICE_CALL)
    }
}

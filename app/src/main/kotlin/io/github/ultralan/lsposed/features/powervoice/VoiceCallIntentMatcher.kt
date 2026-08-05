package io.github.ultralan.lsposed.features.powervoice

import android.content.Intent

object VoiceCallIntentMatcher {
    fun matches(provider: VoiceAssistantProvider, intent: Intent?): Boolean {
        val component = intent?.component ?: return false
        if (component.packageName != provider.packageName) return false
        if (component.className in provider.exactVoiceCallActivities) return true

        val classNameWithoutPackage = component.className
            .removePrefix(provider.packageName)
            .trimStart('.')
        val haystack = listOfNotNull(
            classNameWithoutPackage,
            intent.action,
            intent.dataString,
        ).joinToString(separator = " ").lowercase()

        return provider.voiceCallKeywords.any { keyword ->
            haystack.contains(keyword.lowercase())
        }
    }
}

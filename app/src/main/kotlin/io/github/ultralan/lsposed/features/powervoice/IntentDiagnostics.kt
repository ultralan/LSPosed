package io.github.ultralan.lsposed.features.powervoice

import android.content.Intent

object IntentDiagnostics {
    fun summary(provider: VoiceAssistantProvider, intent: Intent?): String {
        val component = intent?.component
        val matched = VoiceCallIntentMatcher.matches(provider, intent)
        val extrasKeys = intent?.extras
            ?.keySet()
            ?.sorted()
            ?.joinToString(",")
            ?: ""

        return listOf(
            "provider=${provider.displayName}",
            "matched=$matched",
            "reason=${reason(provider, intent)}",
            "component=${component?.let { "${it.packageName}/${it.className}" } ?: ""}",
            "package=${intent?.`package` ?: ""}",
            "action=${intent?.action ?: ""}",
            "data=${intent?.dataString ?: ""}",
            "extrasKeys=$extrasKeys",
        ).joinToString(" ")
    }

    fun shouldLog(provider: VoiceAssistantProvider, intent: Intent?): Boolean {
        if (intent == null) return false
        if (VoiceCallIntentMatcher.matches(provider, intent)) return true
        val component = intent.component
        if (component?.packageName == provider.packageName) return true
        if (intent.`package` == provider.packageName) return true
        val text = listOfNotNull(component?.className, intent.action, intent.dataString)
            .joinToString(" ")
            .lowercase()
        return provider.voiceCallKeywords.any { text.contains(it.lowercase()) }
    }

    private fun reason(provider: VoiceAssistantProvider, intent: Intent?): String {
        if (intent == null) return "intent is null"
        val component = intent.component ?: return "component is null"
        if (component.packageName != provider.packageName) {
            return "component package ${component.packageName} != ${provider.packageName}"
        }
        if (component.className in provider.exactVoiceCallActivities) {
            return "exact voice-call activity matched"
        }
        val classNameWithoutPackage = component.className
            .removePrefix(provider.packageName)
            .trimStart('.')
        val haystack = listOfNotNull(
            classNameWithoutPackage,
            intent.action,
            intent.dataString,
        ).joinToString(separator = " ").lowercase()
        val matchedKeyword = provider.voiceCallKeywords.firstOrNull { keyword ->
            haystack.contains(keyword.lowercase())
        }
        return matchedKeyword?.let { "keyword matched: $it" }
            ?: "provider package matched but no voice-call signal"
    }
}

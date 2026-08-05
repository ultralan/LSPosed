package io.github.ultralan.lsposed.features.powervoice

object ProviderSelector {
    fun select(
        preferredProviderId: String?,
        installedPackageNames: Set<String>,
    ): VoiceAssistantProvider {
        val preferred = preferredProviderId
            ?.let { VoiceAssistantProviders.byId(it) }
            ?.takeIf { it.packageName in installedPackageNames }
        if (preferred != null) return preferred

        return VoiceAssistantProviders.ALL.firstOrNull { it.packageName in installedPackageNames }
            ?: VoiceAssistantProviders.defaultProvider
    }
}

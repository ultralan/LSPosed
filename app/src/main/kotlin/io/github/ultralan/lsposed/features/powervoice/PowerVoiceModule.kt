package io.github.ultralan.lsposed.features.powervoice

import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ultralan.lsposed.core.Config
import io.github.ultralan.lsposed.core.FeatureModule

object PowerVoiceModule : FeatureModule {
    override val id: String = "power_voice"
    override val displayName: String = "电源键语音"
    override val scopes: Set<String> =
        setOf(Config.ANDROID_PACKAGE) + VoiceAssistantProviders.ALL.map { it.packageName }

    override fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == Config.ANDROID_PACKAGE) {
            PowerVoiceSystemHooks.install(lpparam)
            return
        }

        VoiceAssistantProviders.byPackage(lpparam.packageName)
            ?.let { PowerVoiceProviderHooks.install(it) }
    }
}

package io.github.ultralan.lsposed.features.smspush

import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ultralan.lsposed.core.FeatureModule

object SmsPushModule : FeatureModule {
    override val id: String = "sms_push"
    override val displayName: String = "短信拦截推送"
    override val scopes: Set<String> = setOf("com.android.phone")

    override fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        SmsPushSystemHooks.install(lpparam)
    }
}

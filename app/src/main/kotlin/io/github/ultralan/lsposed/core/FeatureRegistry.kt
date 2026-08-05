package io.github.ultralan.lsposed.core

import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ultralan.lsposed.features.powervoice.PowerVoiceModule
import io.github.ultralan.lsposed.features.smspush.SmsPushModule

object FeatureRegistry {
    val all: List<FeatureModule> = listOf(
        PowerVoiceModule,
        SmsPushModule,
    )

    fun modulesForPackage(packageName: String): List<FeatureModule> =
        all.filter { packageName in it.scopes }

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        modulesForPackage(lpparam.packageName).forEach { module ->
            module.install(lpparam)
        }
    }
}

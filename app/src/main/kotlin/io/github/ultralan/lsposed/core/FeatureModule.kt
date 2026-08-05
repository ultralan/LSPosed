package io.github.ultralan.lsposed.core

import de.robv.android.xposed.callbacks.XC_LoadPackage

interface FeatureModule {
    val id: String
    val displayName: String
    val scopes: Set<String>

    fun install(lpparam: XC_LoadPackage.LoadPackageParam)
}

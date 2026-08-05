package io.github.ultralan.lsposed.core

import io.github.ultralan.lsposed.features.powervoice.PowerVoiceModule
import io.github.ultralan.lsposed.features.smspush.SmsPushModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeatureRegistryTest {
    @Test
    fun `android package is routed to power voice module`() {
        val modules = FeatureRegistry.modulesForPackage(Config.ANDROID_PACKAGE)

        assertEquals(listOf(PowerVoiceModule.id), modules.map { it.id })
    }

    @Test
    fun `voice assistant package is routed to power voice module`() {
        val modules = FeatureRegistry.modulesForPackage("com.moonshot.kimichat")

        assertEquals(listOf(PowerVoiceModule.id), modules.map { it.id })
    }

    @Test
    fun `sms push module is routed to phone package`() {
        assertTrue(FeatureRegistry.all.any { it.id == SmsPushModule.id })
        assertEquals(listOf(SmsPushModule.id), FeatureRegistry.modulesForPackage("com.android.phone").map { it.id })
    }
}

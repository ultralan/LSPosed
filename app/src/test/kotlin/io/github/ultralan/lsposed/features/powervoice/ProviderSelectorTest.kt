package io.github.ultralan.lsposed.features.powervoice

import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderSelectorTest {
    @Test
    fun `preferred installed provider wins`() {
        val selected = ProviderSelector.select(
            preferredProviderId = "kimi",
            installedPackageNames = setOf("com.larus.nova", "com.moonshot.kimichat"),
        )

        assertEquals("kimi", selected.id)
    }

    @Test
    fun `uninstalled preferred provider falls back to first installed provider`() {
        val selected = ProviderSelector.select(
            preferredProviderId = "kimi",
            installedPackageNames = setOf("com.iflytek.spark"),
        )

        assertEquals("spark", selected.id)
    }

    @Test
    fun `missing installed provider falls back to default provider`() {
        val selected = ProviderSelector.select(
            preferredProviderId = "kimi",
            installedPackageNames = emptySet(),
        )

        assertEquals("doubao", selected.id)
    }
}

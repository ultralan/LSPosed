package io.github.ultralan.lsposed.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ModuleIdentityTest {
    @Test
    fun `module identity uses lsposed namespace`() {
        assertEquals("LSPosed", Config.TAG)
        assertEquals("io.github.ultralan.lsposed", Config.APPLICATION_ID)
    }
}

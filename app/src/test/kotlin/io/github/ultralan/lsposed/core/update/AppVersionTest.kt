package io.github.ultralan.lsposed.core.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppVersionTest {
    @Test
    fun `newer semantic version is detected from GitHub tag`() {
        assertTrue(AppVersion.isNewer("v0.1.1", "0.1.0"))
        assertTrue(AppVersion.isNewer("v0.2.0", "0.1.9"))
        assertFalse(AppVersion.isNewer("v0.1.0", "0.1.0"))
        assertFalse(AppVersion.isNewer("v0.0.9", "0.1.0"))
    }
}

package io.github.ultralan.lsposed.core

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class ModuleLogStoreTest {
    @Test
    fun `append keeps newest logs first and trims old entries`() {
        val context = RuntimeEnvironment.getApplication()

        ModuleLogStore.clear(context)
        repeat(ModuleLogStore.MAX_ENTRIES + 3) { index ->
            ModuleLogStore.append(context, "测试", "第 $index 条")
        }

        val entries = ModuleLogStore.load(context)
        assertEquals(ModuleLogStore.MAX_ENTRIES, entries.size)
        assertEquals("第 ${ModuleLogStore.MAX_ENTRIES + 2} 条", entries.first().message)
        assertEquals("第 3 条", entries.last().message)
    }
}

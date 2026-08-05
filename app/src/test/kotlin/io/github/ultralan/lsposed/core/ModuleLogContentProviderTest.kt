package io.github.ultralan.lsposed.core

import android.content.ContentValues
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class ModuleLogContentProviderTest {
    @Test
    fun `insert through content provider is visible to app log store`() {
        val context = RuntimeEnvironment.getApplication()
        ModuleLogStore.clear(context)

        context.contentResolver.insert(
            ModuleLogStore.URI,
            ContentValues().apply {
                put(ModuleLogStore.COLUMN_SOURCE, "短信")
                put(ModuleLogStore.COLUMN_MESSAGE, "短信 Hook 捕获：from=10086 body=测试验证码 654321")
                put(ModuleLogStore.COLUMN_TIMESTAMP, 1_000L)
            },
        )

        val entries = ModuleLogStore.load(context)
        assertEquals(1, entries.size)
        assertEquals("短信", entries.first().source)
        assertEquals("短信 Hook 捕获：from=10086 body=测试验证码 654321", entries.first().message)
    }
}

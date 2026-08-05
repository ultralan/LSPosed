package io.github.ultralan.lsposed.core.notification

import android.content.ContentValues
import io.github.ultralan.lsposed.core.ModuleLogStore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class NotificationContentProviderTest {
    @Test
    fun `insert notification event writes dispatch result to module log`() {
        val context = RuntimeEnvironment.getApplication()
        NotificationConfigStore.clear(context)
        ModuleLogStore.clear(context)

        context.contentResolver.insert(
            NotificationEventProvider.URI,
            ContentValues().apply {
                put(NotificationEventProvider.COLUMN_MODULE_ID, NotificationConfigStore.MODULE_SMS_PUSH)
                put(NotificationEventProvider.COLUMN_SOURCE, "短信")
                put(NotificationEventProvider.COLUMN_TITLE, "短信验证码")
                put(NotificationEventProvider.COLUMN_BODY, "验证码 123456")
                put(NotificationEventProvider.COLUMN_COPY_TEXT, "123456")
                put(NotificationEventProvider.COLUMN_DRY_RUN, true)
            },
        )

        val logText = ModuleLogStore.load(context).joinToString("\n") { it.message }
        assertTrue(logText.contains("通知发送完成"))
        assertTrue(logText.contains("短信验证码"))
    }
}

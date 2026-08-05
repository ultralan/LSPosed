package io.github.ultralan.lsposed.core.notification

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class NotificationDispatcherTest {
    @Test
    fun `dispatches only to robots selected by event module`() {
        val context = RuntimeEnvironment.getApplication()
        val sent = mutableListOf<String>()
        NotificationConfigStore.clear(context)
        NotificationConfigStore.saveRobots(
            context,
            listOf(
                NotificationRobot("a", "飞书 A", NotificationRobotType.FEISHU, "https://example.com/a", enabled = true),
                NotificationRobot("b", "飞书 B", NotificationRobotType.FEISHU, "https://example.com/b", enabled = true),
                NotificationRobot("c", "飞书 C", NotificationRobotType.FEISHU, "https://example.com/c", enabled = false),
            ),
        )
        NotificationConfigStore.saveModuleRobotIds(context, NotificationConfigStore.MODULE_SMS_PUSH, setOf("b", "c"))

        val result = NotificationDispatcher.dispatch(
            context,
            NotificationEvent(
                moduleId = NotificationConfigStore.MODULE_SMS_PUSH,
                source = "短信",
                title = "短信验证码",
                body = "验证码 123456",
                copyText = "123456",
            ),
        ) { robot, _ ->
            sent += robot.id
            true
        }

        assertEquals(listOf("b"), sent)
        assertEquals(1, result.sent)
        assertEquals(0, result.failed)
        assertEquals(1, result.skippedDisabled)
    }
}

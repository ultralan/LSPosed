package io.github.ultralan.lsposed.core.notification

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class NotificationConfigStoreTest {
    @Test
    fun `fresh install includes authorized default Feishu robot selected by sms module`() {
        val context = RuntimeEnvironment.getApplication()
        NotificationConfigStore.clear(context)

        val robots = NotificationConfigStore.loadRobots(context)
        val selectedIds = NotificationConfigStore.loadModuleRobotIds(context, NotificationConfigStore.MODULE_SMS_PUSH)

        assertEquals(1, robots.size)
        assertEquals(NotificationConfigStore.DEFAULT_FEISHU_ROBOT_ID, robots.first().id)
        assertEquals("默认飞书机器人", robots.first().name)
        assertEquals(NotificationRobotType.FEISHU, robots.first().type)
        assertEquals(
            "https://open.feishu.cn/open-apis/bot/v2/hook/94ab117c-10e3-4fc4-bdf2-e119a77c0d5d",
            robots.first().webhookUrl,
        )
        assertEquals(true, robots.first().enabled)
        assertEquals(setOf(NotificationConfigStore.DEFAULT_FEISHU_ROBOT_ID), selectedIds)
    }

    @Test
    fun `module robot selection is saved independently from robot list`() {
        val context = RuntimeEnvironment.getApplication()
        NotificationConfigStore.clear(context)
        NotificationConfigStore.saveRobots(
            context,
            listOf(
                NotificationRobot("a", "飞书 A", NotificationRobotType.FEISHU, "https://example.com/a", enabled = true),
                NotificationRobot("b", "飞书 B", NotificationRobotType.FEISHU, "https://example.com/b", enabled = true),
            ),
        )

        NotificationConfigStore.saveModuleRobotIds(context, NotificationConfigStore.MODULE_SMS_PUSH, setOf("b"))

        assertEquals(setOf("b"), NotificationConfigStore.loadModuleRobotIds(context, NotificationConfigStore.MODULE_SMS_PUSH))
        assertEquals(listOf("a", "b"), NotificationConfigStore.loadRobots(context).map { it.id })
    }
}

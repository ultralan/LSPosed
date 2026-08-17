package io.github.ultralan.lsposed.core.notification

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class NotificationRetryStoreTest {
    @Test
    fun `failed delivery persists and retries until it succeeds`() {
        val context = RuntimeEnvironment.getApplication()
        val nowMillis = 1_000L
        NotificationConfigStore.clear(context)
        NotificationConfigStore.saveRobots(
            context,
            listOf(
                NotificationRobot(
                    id = "feishu",
                    name = "飞书",
                    type = NotificationRobotType.FEISHU,
                    webhookUrl = "https://example.com/hook",
                    enabled = true,
                ),
            ),
        )
        NotificationConfigStore.saveModuleRobotIds(
            context,
            NotificationConfigStore.MODULE_SMS_PUSH,
            setOf("feishu"),
        )

        val taskIds = NotificationRetryStore.enqueueSelectedTargets(
            context,
            NotificationEvent(
                moduleId = NotificationConfigStore.MODULE_SMS_PUSH,
                source = "10086",
                title = "短信测试",
                body = "测试正文",
                copyText = null,
            ),
            nowMillis,
        )

        val firstResult = NotificationRetryProcessor.process(context, taskIds, nowMillis) { _, _ -> false }
        val pending = NotificationRetryStore.load(context).single()

        assertEquals(1, firstResult.failed)
        assertEquals(1, firstResult.pending)
        assertEquals(1, pending.attempts)
        assertEquals(nowMillis + 5_000L, pending.nextAttemptAtMillis)

        val retryResult = NotificationRetryProcessor.process(
            context,
            taskIds,
            pending.nextAttemptAtMillis,
        ) { _, _ -> true }

        assertEquals(1, retryResult.sent)
        assertEquals(0, retryResult.pending)
        assertEquals(0, NotificationRetryStore.pendingCount(context))
    }
}

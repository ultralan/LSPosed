package io.github.ultralan.lsposed.core.notification

import android.content.Context

object NotificationDispatcher {
    fun send(robot: NotificationRobot, event: NotificationEvent): Boolean =
        when (robot.type) {
            NotificationRobotType.FEISHU -> FeishuWebhookNotifier.send(robot, event)
        }

    fun dispatch(
        context: Context,
        event: NotificationEvent,
        sender: (NotificationRobot, NotificationEvent) -> Boolean = { robot, notificationEvent ->
            send(robot, notificationEvent)
        },
    ): NotificationDispatchResult {
        val selectedIds = NotificationConfigStore.loadModuleRobotIds(context, event.moduleId)
        if (selectedIds.isEmpty()) {
            return NotificationDispatchResult(sent = 0, failed = 0, skippedDisabled = 0, skippedUnselected = 0)
        }

        var sent = 0
        var failed = 0
        var skippedDisabled = 0
        var skippedUnselected = 0

        NotificationConfigStore.loadRobots(context).forEach { robot ->
            if (robot.id !in selectedIds) {
                skippedUnselected += 1
                return@forEach
            }
            if (!robot.enabled) {
                skippedDisabled += 1
                return@forEach
            }
            if (sender(robot, event)) {
                sent += 1
            } else {
                failed += 1
            }
        }

        return NotificationDispatchResult(sent, failed, skippedDisabled, skippedUnselected)
    }
}

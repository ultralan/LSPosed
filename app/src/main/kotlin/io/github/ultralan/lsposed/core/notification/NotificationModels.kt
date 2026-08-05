package io.github.ultralan.lsposed.core.notification

enum class NotificationRobotType {
    FEISHU,
}

data class NotificationRobot(
    val id: String,
    val name: String,
    val type: NotificationRobotType,
    val webhookUrl: String,
    val enabled: Boolean,
)

data class NotificationEvent(
    val moduleId: String,
    val source: String,
    val title: String,
    val body: String,
    val copyText: String?,
)

data class NotificationDispatchResult(
    val sent: Int,
    val failed: Int,
    val skippedDisabled: Int,
    val skippedUnselected: Int,
)

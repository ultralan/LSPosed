package io.github.ultralan.lsposed.core.notification

import android.content.Context
import android.net.Uri

object NotificationConfigStore {
    const val MODULE_SMS_PUSH = "sms_push"
    const val DEFAULT_FEISHU_ROBOT_ID = "default_feishu"
    private const val DEFAULT_FEISHU_WEBHOOK =
        "https://open.feishu.cn/open-apis/bot/v2/hook/94ab117c-10e3-4fc4-bdf2-e119a77c0d5d"

    private const val PREFS_NAME = "notification_config"
    private const val PREF_KEY_ROBOTS = "robots"
    private const val PREF_KEY_MODULE_PREFIX = "module_robots_"
    private const val FIELD_SEPARATOR = "\u001f"
    private const val ENTRY_SEPARATOR = "\u001e"
    private const val ID_SEPARATOR = ","

    fun loadRobots(context: Context): List<NotificationRobot> {
        val encoded = prefs(context).getString(PREF_KEY_ROBOTS, null)
        if (encoded.isNullOrBlank()) {
            val defaults = defaultRobots()
            saveRobots(context, defaults)
            ensureDefaultSmsSelection(context)
            return defaults
        }
        return encoded.split(ENTRY_SEPARATOR).mapNotNull { decodeRobot(it) }
    }

    fun saveRobots(context: Context, robots: List<NotificationRobot>) {
        prefs(context).edit()
            .putString(PREF_KEY_ROBOTS, robots.joinToString(ENTRY_SEPARATOR) { encodeRobot(it) })
            .commit()
    }

    fun loadModuleRobotIds(context: Context, moduleId: String): Set<String> {
        ensureDefaultSmsSelection(context)
        return prefs(context).getString(PREF_KEY_MODULE_PREFIX + moduleId, null)
            ?.takeIf { it.isNotBlank() }
            ?.split(ID_SEPARATOR)
            ?.map { Uri.decode(it) }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    fun saveModuleRobotIds(context: Context, moduleId: String, robotIds: Set<String>) {
        prefs(context).edit()
            .putString(
                PREF_KEY_MODULE_PREFIX + moduleId,
                robotIds.joinToString(ID_SEPARATOR) { Uri.encode(it) },
            )
            .commit()
    }

    fun upsertRobot(context: Context, robot: NotificationRobot) {
        val next = loadRobots(context)
            .filterNot { it.id == robot.id } + robot
        saveRobots(context, next)
    }

    fun deleteRobot(context: Context, robotId: String) {
        saveRobots(context, loadRobots(context).filterNot { it.id == robotId })
        val smsSelected = loadModuleRobotIds(context, MODULE_SMS_PUSH) - robotId
        saveModuleRobotIds(context, MODULE_SMS_PUSH, smsSelected)
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
        NotificationRetryStore.clear(context)
    }

    fun createRobotId(): String = "robot_${System.currentTimeMillis()}"

    private fun ensureDefaultSmsSelection(context: Context) {
        val preferences = prefs(context)
        if (!preferences.contains(PREF_KEY_MODULE_PREFIX + MODULE_SMS_PUSH)) {
            preferences.edit()
                .putString(PREF_KEY_MODULE_PREFIX + MODULE_SMS_PUSH, Uri.encode(DEFAULT_FEISHU_ROBOT_ID))
                .commit()
        }
    }

    private fun defaultRobots(): List<NotificationRobot> =
        listOf(
            NotificationRobot(
                id = DEFAULT_FEISHU_ROBOT_ID,
                name = "默认飞书机器人",
                type = NotificationRobotType.FEISHU,
                webhookUrl = DEFAULT_FEISHU_WEBHOOK,
                enabled = true,
            ),
        )

    private fun encodeRobot(robot: NotificationRobot): String =
        listOf(
            robot.id,
            robot.name,
            robot.type.name,
            robot.webhookUrl,
            robot.enabled.toString(),
        ).joinToString(FIELD_SEPARATOR) { Uri.encode(it) }

    private fun decodeRobot(encoded: String): NotificationRobot? {
        val parts = encoded.split(FIELD_SEPARATOR)
        if (parts.size != 5) return null
        return NotificationRobot(
            id = Uri.decode(parts[0]),
            name = Uri.decode(parts[1]),
            type = runCatching { NotificationRobotType.valueOf(Uri.decode(parts[2])) }.getOrNull()
                ?: return null,
            webhookUrl = Uri.decode(parts[3]),
            enabled = Uri.decode(parts[4]).toBooleanStrictOrNull() ?: return null,
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

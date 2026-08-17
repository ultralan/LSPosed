package io.github.ultralan.lsposed.core.notification

import android.content.Context
import android.net.Uri

data class PendingNotification(
    val id: String,
    val robotId: String,
    val event: NotificationEvent,
    val attempts: Int,
    val nextAttemptAtMillis: Long,
)

data class NotificationRetryResult(
    val sent: Int,
    val failed: Int,
    val skipped: Int,
    val pending: Int,
)

object NotificationRetryStore {
    private const val PREFS_NAME = "notification_retry"
    private const val PREF_KEY_PENDING = "pending"
    private const val ENTRY_SEPARATOR = "\u001e"
    private const val FIELD_SEPARATOR = "\u001f"
    const val MAX_ATTEMPTS = 5

    private val lock = Any()

    fun enqueueSelectedTargets(
        context: Context,
        event: NotificationEvent,
        nowMillis: Long = System.currentTimeMillis(),
    ): Set<String> = synchronized(lock) {
        val selectedIds = NotificationConfigStore.loadModuleRobotIds(context, event.moduleId)
        val tasks = NotificationConfigStore.loadRobots(context)
            .filter { it.id in selectedIds && it.enabled }
            .map { robot ->
                PendingNotification(
                    id = "notification_${nowMillis}_${robot.id}_${System.nanoTime()}",
                    robotId = robot.id,
                    event = event,
                    attempts = 0,
                    nextAttemptAtMillis = nowMillis,
                )
            }
        if (tasks.isNotEmpty()) save(context, load(context) + tasks)
        tasks.mapTo(linkedSetOf()) { it.id }
    }

    fun load(context: Context): List<PendingNotification> = synchronized(lock) {
        val encoded = prefs(context).getString(PREF_KEY_PENDING, null)
        encoded.orEmpty()
            .split(ENTRY_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull(::decode)
    }

    fun due(context: Context, nowMillis: Long): List<PendingNotification> =
        load(context).filter { it.nextAttemptAtMillis <= nowMillis }

    fun update(context: Context, task: PendingNotification) = synchronized(lock) {
        save(context, load(context).map { current -> if (current.id == task.id) task else current })
    }

    fun remove(context: Context, taskId: String) = synchronized(lock) {
        save(context, load(context).filterNot { it.id == taskId })
    }

    fun nextAttemptAtMillis(context: Context): Long? =
        load(context).minOfOrNull { it.nextAttemptAtMillis }

    fun pendingCount(context: Context): Int = load(context).size

    fun clear(context: Context) = synchronized(lock) {
        prefs(context).edit().remove(PREF_KEY_PENDING).commit()
    }

    private fun save(context: Context, tasks: List<PendingNotification>) {
        prefs(context).edit()
            .putString(PREF_KEY_PENDING, tasks.joinToString(ENTRY_SEPARATOR, transform = ::encode))
            .commit()
    }

    private fun encode(task: PendingNotification): String =
        listOf(
            task.id,
            task.robotId,
            task.event.moduleId,
            task.event.source,
            task.event.title,
            task.event.body,
            task.event.copyText.orEmpty(),
            task.attempts.toString(),
            task.nextAttemptAtMillis.toString(),
        ).joinToString(FIELD_SEPARATOR) { Uri.encode(it) }

    private fun decode(encoded: String): PendingNotification? {
        val fields = encoded.split(FIELD_SEPARATOR)
        if (fields.size != 9) return null
        val attempts = Uri.decode(fields[7]).toIntOrNull() ?: return null
        val nextAttemptAtMillis = Uri.decode(fields[8]).toLongOrNull() ?: return null
        return PendingNotification(
            id = Uri.decode(fields[0]),
            robotId = Uri.decode(fields[1]),
            event = NotificationEvent(
                moduleId = Uri.decode(fields[2]),
                source = Uri.decode(fields[3]),
                title = Uri.decode(fields[4]),
                body = Uri.decode(fields[5]),
                copyText = Uri.decode(fields[6]).ifBlank { null },
            ),
            attempts = attempts,
            nextAttemptAtMillis = nextAttemptAtMillis,
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

object NotificationRetryProcessor {
    private val lock = Any()
    private val retryDelaysMillis = longArrayOf(30_000L, 120_000L, 600_000L, 1_800_000L)

    fun process(
        context: Context,
        taskIds: Set<String>? = null,
        nowMillis: Long = System.currentTimeMillis(),
        sender: (NotificationRobot, NotificationEvent) -> Boolean = NotificationDispatcher::send,
    ): NotificationRetryResult = synchronized(lock) {
        val dueTasks = NotificationRetryStore.due(context, nowMillis)
            .filter { taskIds == null || it.id in taskIds }
        val robots = NotificationConfigStore.loadRobots(context).associateBy { it.id }
        var sent = 0
        var failed = 0
        var skipped = 0

        dueTasks.forEach { task ->
            val robot = robots[task.robotId]
            if (robot == null || !robot.enabled) {
                NotificationRetryStore.remove(context, task.id)
                skipped += 1
                return@forEach
            }

            if (sender(robot, task.event)) {
                NotificationRetryStore.remove(context, task.id)
                sent += 1
                return@forEach
            }

            val attempts = task.attempts + 1
            if (attempts >= NotificationRetryStore.MAX_ATTEMPTS) {
                NotificationRetryStore.remove(context, task.id)
                failed += 1
                return@forEach
            }
            NotificationRetryStore.update(
                context,
                task.copy(
                    attempts = attempts,
                    nextAttemptAtMillis = nowMillis + retryDelaysMillis[attempts - 1],
                ),
            )
            failed += 1
        }

        NotificationRetryResult(
            sent = sent,
            failed = failed,
            skipped = skipped,
            pending = NotificationRetryStore.pendingCount(context),
        )
    }
}

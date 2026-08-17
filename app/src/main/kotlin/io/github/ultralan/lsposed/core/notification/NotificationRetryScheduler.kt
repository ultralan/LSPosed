package io.github.ultralan.lsposed.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ultralan.lsposed.core.Log
import io.github.ultralan.lsposed.core.ModuleLogStore

object NotificationRetryScheduler {
    private const val ACTION_RETRY = "io.github.ultralan.lsposed.notification.RETRY"
    private const val POLL_INTERVAL_MILLIS = 15 * 60 * 1_000L
    private const val REQUEST_CODE = 2001

    fun processAsync(
        context: Context,
        taskIds: Set<String>? = null,
        onComplete: ((NotificationRetryResult) -> Unit)? = null,
    ) {
        val appContext = context.applicationContext
        Thread({
            val result = runCatching {
                NotificationRetryProcessor.process(appContext, taskIds)
                    .also { scheduleNext(appContext) }
            }.onFailure { error ->
                Log.e("通知重试调度失败", error)
            }.getOrElse {
                NotificationRetryResult(sent = 0, failed = 0, skipped = 0, pending = 0)
            }
            runCatching { onComplete?.invoke(result) }
        }, "LSPosedNotificationRetry").start()
    }

    fun scheduleNext(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = retryPendingIntent(appContext)
        val nextAttemptAtMillis = NotificationRetryStore.nextAttemptAtMillis(appContext)
        if (nextAttemptAtMillis == null) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val triggerAtMillis = minOf(nextAttemptAtMillis, System.currentTimeMillis() + POLL_INTERVAL_MILLIS)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    fun logRetryResult(context: Context, result: NotificationRetryResult) {
        if (result.sent == 0 && result.failed == 0 && result.skipped == 0) return
        ModuleLogStore.append(
            context,
            "通知服务",
            "通知重试：成功 ${result.sent}，失败 ${result.failed}，跳过 ${result.skipped}，待重试 ${result.pending}",
        )
    }

    private fun retryPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, NotificationRetryReceiver::class.java).setAction(ACTION_RETRY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    internal fun isRetryAction(intent: Intent): Boolean = intent.action == ACTION_RETRY
}

class NotificationRetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!NotificationRetryScheduler.isRetryAction(intent)) return
        val pendingResult = goAsync()
        NotificationRetryScheduler.processAsync(context) { result ->
            try {
                NotificationRetryScheduler.logRetryResult(context.applicationContext, result)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class NotificationRetryBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        NotificationRetryScheduler.processAsync(context) { result ->
            try {
                NotificationRetryScheduler.logRetryResult(context.applicationContext, result)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

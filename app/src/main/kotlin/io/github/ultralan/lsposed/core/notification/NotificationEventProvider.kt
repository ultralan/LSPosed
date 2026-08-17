package io.github.ultralan.lsposed.core.notification

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import io.github.ultralan.lsposed.core.Config
import io.github.ultralan.lsposed.core.ModuleLogStore

class NotificationEventProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { NotificationRetryScheduler.processAsync(it) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uri != URI || values == null) return null
        val appContext = context ?: return null
        val event = NotificationEvent(
            moduleId = values.getAsString(COLUMN_MODULE_ID) ?: return null,
            source = values.getAsString(COLUMN_SOURCE) ?: "模块",
            title = values.getAsString(COLUMN_TITLE) ?: "模块通知",
            body = values.getAsString(COLUMN_BODY) ?: return null,
            copyText = values.getAsString(COLUMN_COPY_TEXT),
        )

        if (values.getAsBoolean(COLUMN_DRY_RUN) == true) {
            val result = NotificationRetryResult(sent = 1, failed = 0, skipped = 0, pending = 0)
            logResult(appContext, event, result)
            return uri
        }

        val taskIds = NotificationRetryStore.enqueueSelectedTargets(appContext, event)
        NotificationRetryScheduler.processAsync(appContext, taskIds) { result ->
            logResult(appContext, event, result)
        }
        return uri
    }

    private fun logResult(
        context: android.content.Context,
        event: NotificationEvent,
        result: NotificationRetryResult,
    ) {
        ModuleLogStore.append(
            context,
            "通知服务",
            if (result.sent == 0 && result.failed == 0 && result.pending == 0) {
                "通知未发送：${event.title}，模块未选择启用机器人"
            } else {
                "通知发送完成：${event.title}，成功 ${result.sent}，失败 ${result.failed}，待重试 ${result.pending}"
            },
        )
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "${Config.APPLICATION_ID}.notification.events"
        const val PATH_EVENTS = "events"
        const val COLUMN_MODULE_ID = "module_id"
        const val COLUMN_SOURCE = "source"
        const val COLUMN_TITLE = "title"
        const val COLUMN_BODY = "body"
        const val COLUMN_COPY_TEXT = "copy_text"
        const val COLUMN_DRY_RUN = "dry_run"

        val URI: Uri = Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .appendPath(PATH_EVENTS)
            .build()
    }
}

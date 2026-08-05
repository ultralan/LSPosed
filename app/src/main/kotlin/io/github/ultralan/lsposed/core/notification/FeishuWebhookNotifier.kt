package io.github.ultralan.lsposed.core.notification

import java.net.HttpURLConnection
import java.net.URL

object FeishuWebhookNotifier {
    fun send(robot: NotificationRobot, event: NotificationEvent): Boolean =
        runCatching {
            val connection = (URL(robot.webhookUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6_000
                readTimeout = 6_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use { stream ->
                stream.write(buildPayload(event).toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            connection.disconnect()
            code in 200..299
        }.getOrDefault(false)

    fun buildPayload(event: NotificationEvent): String {
        val headerTemplate = if (event.copyText.isNullOrBlank()) "blue" else "red"
        return """
            {"msg_type":"interactive","card":{"config":{"wide_screen_mode":true},"header":{"title":{"tag":"plain_text","content":"${json(event.title)}"},"template":"$headerTemplate"},"elements":[{"tag":"div","text":{"tag":"lark_md","content":"**来源**：${json(event.source)}\n${json(event.body)}"}}]}}
        """.trimIndent().replace("\n", "")
    }

    private fun json(value: String): String =
        buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
}

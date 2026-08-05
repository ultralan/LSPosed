package io.github.ultralan.lsposed.features.smspush

data class SmsNotificationContent(
    val source: String,
    val title: String,
    val body: String,
    val copyText: String?,
)

object SmsNotificationFormatter {
    fun format(message: SmsMessageSnapshot): SmsNotificationContent {
        val sender = message.sender?.trim()?.takeIf { it.isNotEmpty() } ?: "未知号码"
        val verificationCode = SmsCodeExtractor.extract(message.body)
        return SmsNotificationContent(
            source = sender,
            title = verificationCode?.let { "验证码：$it" } ?: "短信：$sender",
            body = message.body,
            copyText = verificationCode,
        )
    }
}

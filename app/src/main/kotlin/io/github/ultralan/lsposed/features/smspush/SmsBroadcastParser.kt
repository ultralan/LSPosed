package io.github.ultralan.lsposed.features.smspush

import android.content.Intent
import android.provider.Telephony

data class SmsMessageSnapshot(
    val sender: String?,
    val body: String,
)

object SmsBroadcastParser {
    private val supportedActions = setOf(
        Telephony.Sms.Intents.SMS_RECEIVED_ACTION,
        Telephony.Sms.Intents.SMS_DELIVER_ACTION,
        Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION,
    )

    fun parse(intent: Intent): SmsMessageSnapshot? {
        if (intent.action !in supportedActions) return null

        fromPdus(intent)?.let { return it }
        return fromRawExtras(intent)
    }

    private fun fromPdus(intent: Intent): SmsMessageSnapshot? =
        runCatching {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                .filterNotNull()
            if (messages.isEmpty()) return@runCatching null

            SmsMessageSnapshot(
                sender = messages.firstNotNullOfOrNull {
                    it.displayOriginatingAddress ?: it.originatingAddress
                },
                body = messages.joinToString(separator = "") {
                    it.displayMessageBody ?: it.messageBody ?: ""
                }.trim(),
            ).takeIf { it.body.isNotBlank() }
        }.getOrNull()

    private fun fromRawExtras(intent: Intent): SmsMessageSnapshot? {
        val body = intent.getStringExtra("messageBody")
            ?: intent.getStringExtra("body")
            ?: intent.getStringExtra("message")
            ?: return null

        return SmsMessageSnapshot(
            sender = intent.getStringExtra("originatingAddress")
                ?: intent.getStringExtra("sender")
                ?: intent.getStringExtra("address"),
            body = body,
        )
    }
}

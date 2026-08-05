package io.github.ultralan.lsposed.features.smspush

import android.content.Intent
import android.provider.Telephony
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class SmsBroadcastParserTest {
    @Test
    fun `ignores non sms intents`() {
        val message = SmsBroadcastParser.parse(Intent("android.intent.action.BOOT_COMPLETED"))

        assertNull(message)
    }

    @Test
    fun `parses raw sms extras fallback`() {
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            .putExtra("originatingAddress", "+8613800138000")
            .putExtra("messageBody", "验证码 123456，请勿泄露")

        val message = SmsBroadcastParser.parse(intent)

        assertEquals(
            SmsMessageSnapshot(
                sender = "+8613800138000",
                body = "验证码 123456，请勿泄露",
            ),
            message,
        )
    }
}

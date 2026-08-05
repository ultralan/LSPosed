package io.github.ultralan.lsposed.features.smspush

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SmsNotificationFormatterTest {
    @Test
    fun `verification code appears directly in notification title`() {
        val content = SmsNotificationFormatter.format(
            SmsMessageSnapshot(
                sender = "10086",
                body = "您的登录验证码为 123456，请勿泄露。",
            ),
        )

        assertEquals("验证码：123456", content.title)
        assertEquals("10086", content.source)
        assertEquals("您的登录验证码为 123456，请勿泄露。", content.body)
        assertEquals("123456", content.copyText)
    }

    @Test
    fun `ordinary sms keeps sender in title and forwards original body`() {
        val content = SmsNotificationFormatter.format(
            SmsMessageSnapshot(
                sender = "顺丰速运",
                body = "您的快件已放至丰巢柜，请及时领取。",
            ),
        )

        assertEquals("短信：顺丰速运", content.title)
        assertEquals("顺丰速运", content.source)
        assertEquals("您的快件已放至丰巢柜，请及时领取。", content.body)
        assertNull(content.copyText)
    }
}

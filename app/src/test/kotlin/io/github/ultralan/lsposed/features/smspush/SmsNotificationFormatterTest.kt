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
    fun `ordinary sms puts bracketed brand and core content in title`() {
        val content = SmsNotificationFormatter.format(
            SmsMessageSnapshot(
                sender = "10690000",
                body = "【顺丰速运】尊敬的用户，您的快件已放至丰巢柜，请及时领取。",
            ),
        )

        assertEquals("顺丰速运：您的快件已放至丰巢柜，请及时领取", content.title)
        assertEquals("10690000", content.source)
        assertEquals("【顺丰速运】尊敬的用户，您的快件已放至丰巢柜，请及时领取。", content.body)
        assertNull(content.copyText)
    }

    @Test
    fun `ordinary sms uses sender and first sentence when it has no bracketed brand`() {
        val content = SmsNotificationFormatter.format(
            SmsMessageSnapshot(
                sender = "招商银行",
                body = "您的信用卡尾号1234于08月05日消费88.00元，请留意账单。",
            ),
        )

        assertEquals("招商银行：您的信用卡尾号1234于08月05日消费88.00元，请留意…", content.title)
        assertNull(content.copyText)
    }

    @Test
    fun `ordinary sms title truncates long content without losing the sender`() {
        val content = SmsNotificationFormatter.format(
            SmsMessageSnapshot(
                sender = "业务通知",
                body = "您的会员服务将在明天到期，请尽快打开应用完成续费以免影响正常使用，感谢您的支持。",
            ),
        )

        assertEquals("业务通知：您的会员服务将在明天到期，请尽快打开应用完成续费以免影响正常…", content.title)
    }
}

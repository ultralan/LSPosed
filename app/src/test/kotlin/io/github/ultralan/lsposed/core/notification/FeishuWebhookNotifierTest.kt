package io.github.ultralan.lsposed.core.notification

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class FeishuWebhookNotifierTest {
    @Test
    fun `builds interactive card payload with highlighted verification code`() {
        val payload = FeishuWebhookNotifier.buildPayload(
            NotificationEvent(
                moduleId = NotificationConfigStore.MODULE_SMS_PUSH,
                source = "短信",
                title = "验证码：123456",
                body = "您的登录验证码为 123456，请勿泄露。",
                copyText = "123456",
            ),
        )

        assertContains(payload, "\"msg_type\":\"interactive\"")
        assertContains(payload, "验证码：123456")
        assertContains(payload, "您的登录验证码为 123456，请勿泄露。")
        assertContains(payload, "\"template\":\"red\"")
        assertFalse(payload.contains("验证码：**`123456`**"))
        assertFalse(payload.contains("\"tag\":\"button\""))
        assertFalse(payload.contains("\"tag\":\"action\""))
        assertFalse(payload.contains("复制验证码"))
    }

    @Test
    fun `ordinary sms uses normal card color`() {
        val payload = FeishuWebhookNotifier.buildPayload(
            NotificationEvent(
                moduleId = NotificationConfigStore.MODULE_SMS_PUSH,
                source = "顺丰速运",
                title = "短信：顺丰速运",
                body = "您的快件已放至丰巢柜，请及时领取。",
                copyText = null,
            ),
        )

        assertContains(payload, "短信：顺丰速运")
        assertContains(payload, "您的快件已放至丰巢柜，请及时领取。")
        assertContains(payload, "\"template\":\"blue\"")
    }
}

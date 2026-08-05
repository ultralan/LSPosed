package io.github.ultralan.lsposed.features.smspush

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SmsCodeExtractorTest {
    @Test
    fun `extracts code near verification keyword`() {
        assertEquals("123456", SmsCodeExtractor.extract("您的登录验证码为 123456，请勿泄露。"))
        assertEquals("A9B2", SmsCodeExtractor.extract("A9B2 是您的动态码，五分钟内有效。"))
    }

    @Test
    fun `does not treat ordinary business number as verification code`() {
        assertNull(SmsCodeExtractor.extract("订单号 123456 已发货，请注意查收。"))
        assertNull(SmsCodeExtractor.extract("您的快件已存入 9527 号柜。"))
    }
}

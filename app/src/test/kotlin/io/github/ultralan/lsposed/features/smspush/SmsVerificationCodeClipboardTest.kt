package io.github.ultralan.lsposed.features.smspush

import android.content.ClipboardManager
import android.content.Context
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class SmsVerificationCodeClipboardTest {
    @Test
    fun `copies only the verification code as plain text`() {
        val context = RuntimeEnvironment.getApplication()
        val clipboard = assertNotNull(
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager,
        )

        assertTrue(SmsVerificationCodeClipboard.copy(context, "A9B2"))
        assertEquals("A9B2", clipboard.primaryClip?.getItemAt(0)?.coerceToText(context))
    }
}

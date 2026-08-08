package io.github.ultralan.lsposed.features.smspush

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object SmsVerificationCodeClipboard {
    fun copy(context: Context, verificationCode: String): Boolean = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("验证码", verificationCode))
        true
    }.getOrDefault(false)
}

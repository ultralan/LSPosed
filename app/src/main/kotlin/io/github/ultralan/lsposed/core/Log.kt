package io.github.ultralan.lsposed.core

import de.robv.android.xposed.XposedBridge

object Log {
    fun i(message: String) {
        XposedBridge.log("${Config.TAG}: $message")
    }

    fun e(message: String, throwable: Throwable? = null) {
        XposedBridge.log("${Config.TAG}: $message${throwable?.let { " / $it" } ?: ""}")
    }
}

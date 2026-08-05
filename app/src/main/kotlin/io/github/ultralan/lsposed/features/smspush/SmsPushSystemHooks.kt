package io.github.ultralan.lsposed.features.smspush

import android.content.Intent
import android.content.ContentValues
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ultralan.lsposed.core.Log
import io.github.ultralan.lsposed.core.ModuleLogStore
import io.github.ultralan.lsposed.core.notification.NotificationConfigStore
import io.github.ultralan.lsposed.core.notification.NotificationEventProvider

object SmsPushSystemHooks {
    private const val INBOUND_SMS_HANDLER = "com.android.internal.telephony.InboundSmsHandler"

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching { XposedHelpers.findClass(INBOUND_SMS_HANDLER, lpparam.classLoader) }
            .onSuccess { handlerClass ->
                XposedBridge.hookAllMethods(handlerClass, "dispatchIntent", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args.firstNotNullOfOrNull { it as? Intent } ?: return
                        SmsBroadcastParser.parse(intent)?.let { message ->
                            val notification = SmsNotificationFormatter.format(message)
                            val text = "短信 Hook 捕获：from=${message.sender ?: "未知"} body=${message.body}"
                            Log.i(text)
                            runCatching {
                                val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as? android.content.Context
                                if (context != null) {
                                    ModuleLogStore.appendThroughProvider(context, "短信", text)
                                    context.contentResolver.insert(
                                        NotificationEventProvider.URI,
                                        ContentValues().apply {
                                            put(NotificationEventProvider.COLUMN_MODULE_ID, NotificationConfigStore.MODULE_SMS_PUSH)
                                            put(NotificationEventProvider.COLUMN_SOURCE, notification.source)
                                            put(NotificationEventProvider.COLUMN_TITLE, notification.title)
                                            put(NotificationEventProvider.COLUMN_BODY, notification.body)
                                            put(NotificationEventProvider.COLUMN_COPY_TEXT, notification.copyText)
                                        },
                                    )
                                }
                            }.onFailure { Log.e("写入应用日志失败", it) }
                        }
                    }
                })
                Log.i("短信模块已 hook: $INBOUND_SMS_HANDLER#dispatchIntent")
            }
            .onFailure { Log.e("短信模块未找到类: $INBOUND_SMS_HANDLER", it) }
    }
}

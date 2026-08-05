package io.github.ultralan.lsposed.features.powervoice

import android.app.Activity
import android.app.AndroidAppHelper
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.ultralan.lsposed.core.Log
import java.lang.reflect.Method

object PowerVoiceProviderHooks {
    private val installedProviderIds = mutableSetOf<String>()
    private var lastTriggerAt = 0L

    fun install(provider: VoiceAssistantProvider) {
        if (!installedProviderIds.add(provider.id)) return
        hookActivityTrigger(provider)
        hookVoiceCallLearning(provider)
        Log.i("${provider.displayName} 侧 hook 已安装")
    }

    private fun hookActivityTrigger(provider: VoiceAssistantProvider) {
        XposedBridge.hookAllMethods(Activity::class.java, "onCreate", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val activity = param.thisObject as Activity
                maybeStartVoiceCall(provider, activity, activity.intent)
            }
        })

        XposedBridge.hookAllMethods(Activity::class.java, "onNewIntent", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                maybeStartVoiceCall(provider, param.thisObject as Activity, param.args[0] as? Intent)
            }
        })

        XposedBridge.hookAllMethods(Activity::class.java, "onResume", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val activity = param.thisObject as Activity
                maybeStartVoiceCall(provider, activity, activity.intent)
            }
        })
    }

    private fun hookVoiceCallLearning(provider: VoiceAssistantProvider) {
        Instrumentation::class.java.declaredMethods
            .filter { it.name == "execStartActivity" }
            .forEach { method ->
                val intentIndex = method.intentArgIndex() ?: return@forEach
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[intentIndex] as? Intent ?: return
                        if (IntentDiagnostics.shouldLog(provider, intent)) {
                            Log.i("候选语音通话 Intent: ${IntentDiagnostics.summary(provider, intent)}")
                        }
                        if (!VoiceCallIntentMatcher.matches(provider, intent)) return

                        val context = AndroidAppHelper.currentApplication() ?: run {
                            Log.e("${provider.displayName} 学习失败：currentApplication 为空")
                            return
                        }
                        IntentStore.save(context, provider, Intent(intent))
                        LearnedVoiceCallIntentStore.publish(context, provider, Intent(intent))
                        Log.i("已学习 ${provider.displayName} 语音通话 Intent: ${intent.toUri(Intent.URI_INTENT_SCHEME)}")
                    }
                })
            }
    }

    private fun maybeStartVoiceCall(
        provider: VoiceAssistantProvider,
        activity: Activity,
        triggerIntent: Intent?,
    ) {
        if (!triggerIntent.isVoiceCallTriggerFor(provider)) return
        triggerIntent.clearVoiceCallTrigger()
        if (isDuplicateTrigger()) return

        val learned = LearnedVoiceCallIntentStore.queryOrMigrateLocal(activity, provider)
            ?: run {
                Log.e("未找到已学习的 ${provider.displayName} 语音通话 Intent，请先手动点一次语音通话")
                return
            }

        learned.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Log.i("复用已学习 Intent 启动 ${provider.displayName} 语音通话")
        activity.startActivity(learned)
    }

    private fun Intent?.isVoiceCallTriggerFor(provider: VoiceAssistantProvider): Boolean {
        if (this == null) return false
        val providerId = getStringExtra(TriggerExtras.EXTRA_TRIGGER_PROVIDER_ID)
        val modernTrigger = getBooleanExtra(TriggerExtras.EXTRA_TRIGGER_VOICE_CALL, false) &&
            providerId == provider.id
        val legacyDoubaoTrigger = provider.id == VoiceAssistantProviders.defaultProvider.id &&
            getBooleanExtra(PowerVoiceConfig.EXTRA_TRIGGER_DOUBAO_VOICE_CALL, false)
        return modernTrigger || legacyDoubaoTrigger
    }

    private fun Intent?.clearVoiceCallTrigger() {
        this ?: return
        removeExtra(TriggerExtras.EXTRA_TRIGGER_VOICE_CALL)
        removeExtra(TriggerExtras.EXTRA_TRIGGER_PROVIDER_ID)
        removeExtra(PowerVoiceConfig.EXTRA_TRIGGER_DOUBAO_VOICE_CALL)
    }

    private fun isDuplicateTrigger(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastTriggerAt < 1_500L) return true
        lastTriggerAt = now
        return false
    }

    private fun Method.intentArgIndex(): Int? =
        parameterTypes.indexOfFirst { it == Intent::class.java }.takeIf { it >= 0 }
}

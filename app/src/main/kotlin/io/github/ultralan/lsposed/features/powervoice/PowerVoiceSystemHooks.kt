package io.github.ultralan.lsposed.features.powervoice

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ultralan.lsposed.core.Config
import io.github.ultralan.lsposed.core.Log

object PowerVoiceSystemHooks {
    private var lastTriggerAt = 0L

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookPowerLongPress(lpparam, "com.android.server.policy.PhoneWindowManager")
        hookPowerLongPress(lpparam, "com.android.server.policy.MzPhoneWindowManager")
    }

    private fun hookPowerLongPress(lpparam: XC_LoadPackage.LoadPackageParam, className: String) {
        runCatching { XposedHelpers.findClass(className, lpparam.classLoader) }
            .onSuccess { policyClass ->
                XposedBridge.hookAllMethods(policyClass, "powerLongPress", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val policy = param.thisObject
                        val context = policy.contextOrNull() ?: run {
                            Log.e("系统侧启动失败：未找到 Context")
                            return
                        }
                        val preferredProviderId = preferredProviderId(context)
                        if (TargetProviderStore.isSystemDefault(preferredProviderId)) return

                        if (!isDuplicateTrigger()) startProvider(context, preferredProviderId)
                        policy.markPowerHandled()
                        param.result = null
                    }
                })
                Log.i("系统侧已 hook: $className")
            }
            .onFailure { Log.e("系统侧未找到类: $className", it) }
    }

    private fun startProvider(context: Context, preferredProviderId: String?) {
        val provider = selectProvider(context, preferredProviderId)
        val intent = PowerVoiceLaunchIntentFactory.create(context, provider) ?: run {
            Log.e("系统侧启动失败：未找到 ${provider.displayName} 启动入口")
            return
        }

        Log.i("系统侧准备启动 ${provider.displayName}")
        runCatching {
            XposedHelpers.callMethod(context, "startActivityAsUser", intent, android.os.Process.myUserHandle())
        }.onFailure {
            Log.e("startActivityAsUser 失败，回退 startActivity", it)
            context.startActivity(intent)
        }
    }

    private fun selectProvider(
        context: Context,
        preferredProviderId: String?,
    ): VoiceAssistantProvider =
        ProviderSelector.select(
            preferredProviderId = preferredProviderId,
            installedPackageNames = VoiceAssistantProviders.ALL
                .filter { it.isInstalled(context) }
                .map { it.packageName }
                .toSet(),
        )

    private fun VoiceAssistantProvider.isInstalled(context: Context): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrElse { false }

    private fun preferredProviderId(context: Context): String? =
        queryPreferredProviderId(context)
            ?: sharedPreferencesProviderId()

    private fun queryPreferredProviderId(context: Context): String? =
        TargetProviderStore.query(context)
            ?.also { Log.i("系统侧通过 ContentProvider 读取目标 Provider: $it") }

    private fun sharedPreferencesProviderId(): String? =
        runCatching {
            val providerId = XSharedPreferences(Config.APPLICATION_ID, PowerVoiceConfig.PREFS_NAME).apply {
                reload()
            }.getString(PowerVoiceConfig.PREF_KEY_TARGET_PROVIDER_ID, null)
            Log.i("系统侧读取目标 Provider: ${providerId ?: "未配置"}")
            providerId
        }.onFailure {
            Log.e("读取目标 Provider 配置失败", it)
        }.getOrNull()

    private fun Any.contextOrNull(): Context? =
        runCatching { XposedHelpers.getObjectField(this, "mContext") as? Context }.getOrNull()

    private fun Any.markPowerHandled() {
        runCatching { XposedHelpers.setBooleanField(this, "mPowerKeyHandled", true) }
            .onFailure { Log.e("设置 mPowerKeyHandled 失败", it) }
    }

    private fun isDuplicateTrigger(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastTriggerAt < 1_500L) return true
        lastTriggerAt = now
        return false
    }
}

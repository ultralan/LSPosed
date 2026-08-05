package io.github.ultralan.lsposed

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.ultralan.lsposed.core.ModuleLogStore
import io.github.ultralan.lsposed.core.notification.NotificationConfigStore
import io.github.ultralan.lsposed.core.notification.NotificationRobot
import io.github.ultralan.lsposed.core.notification.NotificationRobotType
import io.github.ultralan.lsposed.core.update.AppUpdateClient
import io.github.ultralan.lsposed.core.update.AppVersion
import io.github.ultralan.lsposed.core.update.GitHubRelease
import io.github.ultralan.lsposed.core.update.UpdateApkContentProvider
import io.github.ultralan.lsposed.features.powervoice.ReadablePreferences
import io.github.ultralan.lsposed.features.powervoice.TargetProviderStore
import io.github.ultralan.lsposed.features.powervoice.VoiceAssistantProvider
import io.github.ultralan.lsposed.features.powervoice.VoiceAssistantProviders

class MainActivity : Activity() {
    private enum class Screen {
        HOME,
        POWER_VOICE,
        SMS_PUSH,
        NOTIFICATION,
        LOGS,
        UPDATE,
    }

    private var currentScreen = Screen.HOME
    private var updateStatus = "通过 GitHub Releases 获取最新正式版本。"
    private var availableRelease: GitHubRelease? = null
    private var pendingInstallPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReadablePreferences.makeTargetProviderPreferencesReadable(this)
        renderHome()
    }

    override fun onResume() {
        super.onResume()
        if (pendingInstallPermission && canInstallPackages()) {
            pendingInstallPermission = false
            launchInstaller()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (currentScreen == Screen.HOME) {
            super.onBackPressed()
        } else {
            renderHome()
        }
    }

    private fun renderHome() {
        currentScreen = Screen.HOME
        setContentView(page {
            addTitle("LSPosed", "模块设置台")
            addEntry(
                title = "电源键语音",
                summary = "当前目标：${selectedProvider().displayName}",
                actionText = "配置电源键语音",
            ) { renderPowerVoice() }
            addEntry(
                title = "短信拦截推送",
                summary = "已选择 ${NotificationConfigStore.loadModuleRobotIds(this@MainActivity, NotificationConfigStore.MODULE_SMS_PUSH).size} 个通知机器人",
                actionText = "配置短信拦截推送",
            ) { renderSmsPush() }
            addEntry(
                title = "通知服务",
                summary = "${NotificationConfigStore.loadRobots(this@MainActivity).count { it.enabled }} 个启用机器人",
                actionText = "配置通知服务",
            ) { renderNotificationService() }
            addEntry(
                title = "运行日志",
                summary = "最近 ${ModuleLogStore.load(this@MainActivity).size} 条模块记录",
                actionText = "查看运行日志",
            ) { renderLogs() }
            addEntry(
                title = "应用更新",
                summary = "当前版本：${currentVersionName()}",
                actionText = "检查应用更新",
            ) { renderUpdate() }
        })
    }

    private fun renderPowerVoice() {
        currentScreen = Screen.POWER_VOICE
        setContentView(page {
            addHeader("电源键语音", "选择目标应用")
            addBodyText(
                """
                在 LSPosed 中勾选 android 和目标 AI 应用后，长按电源键会进入已学习的语音通话入口。
                """.trimIndent(),
            )
            addStatus("当前目标：${selectedProvider().displayName}")
            VoiceAssistantProviders.ALL.forEach { provider ->
                addButton("${provider.displayName} (${provider.packageName})") {
                    saveTargetProvider(provider)
                    renderPowerVoice()
                }
            }
        })
    }

    private fun renderSmsPush() {
        currentScreen = Screen.SMS_PUSH
        setContentView(page {
            addHeader("短信拦截推送", "短信 Hook")
            addStatus("Hook 作用域：com.android.phone")
            addBodyText(
                """
                当前阶段会 hook 系统短信分发入口，收到短信后写入 LSPosed 日志和应用内运行日志。

                测试前请在 LSPosed 中启用本模块，并确认作用域包含 com.android.phone，然后软重启或重启手机。
                """.trimIndent(),
            )
            addBodyText("选择短信模块要推送到的机器人：")
            addRobotSelection(NotificationConfigStore.MODULE_SMS_PUSH)
        })
    }

    private fun renderNotificationService() {
        currentScreen = Screen.NOTIFICATION
        setContentView(page {
            addHeader("通知服务", "飞书机器人")
            val robots = NotificationConfigStore.loadRobots(this@MainActivity)
            addBodyText("公共通知服务会被各模块复用。每个模块可以单独选择要推送到哪些机器人。")
            if (robots.isEmpty()) {
                addBodyText("暂无通知机器人。")
            }
            robots.forEach { robot ->
                addRobotRow(robot)
            }
            addFeishuRobotForm()
        })
    }

    private fun renderLogs() {
        currentScreen = Screen.LOGS
        setContentView(page {
            addHeader("运行日志", "模块日志")
            val entries = ModuleLogStore.load(this@MainActivity)
            addButton("刷新") { renderLogs() }
            addButton("清空日志") {
                ModuleLogStore.clear(this@MainActivity)
                renderLogs()
            }
            if (entries.isEmpty()) {
                addBodyText("暂无日志。收到短信或模块执行动作后会出现在这里。")
            } else {
                entries.forEach { entry ->
                    addLogLine(entry.displayText())
                }
            }
        })
    }

    private fun renderUpdate() {
        currentScreen = Screen.UPDATE
        setContentView(page {
            addHeader("应用更新", "GitHub Releases")
            addStatus("当前版本：${currentVersionName()}")
            addBodyText(updateStatus)
            addButton("检查更新") { checkForUpdate() }
            availableRelease?.let { release ->
                addStatus("可用版本：${release.tagName}")
                if (release.releaseNotes.isNotBlank()) {
                    addBodyText(release.releaseNotes)
                }
                addButton("下载并安装 ${release.tagName}") {
                    downloadAndInstall(release)
                }
            }
        })
    }

    private fun checkForUpdate() {
        availableRelease = null
        updateStatus = "正在检查 GitHub 最新版本……"
        renderUpdate()
        val currentVersion = currentVersionName()
        Thread({
            runCatching { AppUpdateClient.fetchLatestRelease() }
                .onSuccess { release ->
                    runOnUiThread {
                        if (AppVersion.isNewer(release.tagName, currentVersion)) {
                            availableRelease = release
                            updateStatus = "发现新版本，可以直接下载并进入系统安装。"
                        } else {
                            updateStatus = "当前已经是最新版本。"
                        }
                        renderUpdate()
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        updateStatus = "检查更新失败：${error.message ?: error.javaClass.simpleName}"
                        renderUpdate()
                    }
                }
        }, "LSPosedUpdateChecker").start()
    }

    private fun downloadAndInstall(release: GitHubRelease) {
        updateStatus = "正在下载并校验 ${release.tagName}……"
        renderUpdate()
        Thread({
            runCatching { AppUpdateClient.downloadAndVerify(this, release) }
                .onSuccess {
                    runOnUiThread {
                        updateStatus = "更新包校验通过，准备进入系统安装。"
                        renderUpdate()
                        requestInstallPermissionOrLaunch()
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        updateStatus = "下载更新失败：${error.message ?: error.javaClass.simpleName}"
                        renderUpdate()
                    }
                }
        }, "LSPosedUpdateDownloader").start()
    }

    private fun requestInstallPermissionOrLaunch() {
        if (!canInstallPackages()) {
            pendingInstallPermission = true
            updateStatus = "请允许 LSPosed 安装未知应用，返回后将继续安装。"
            renderUpdate()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName"),
                ),
            )
            return
        }
        launchInstaller()
    }

    private fun canInstallPackages(): Boolean = packageManager.canRequestPackageInstalls()

    private fun launchInstaller() {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(UpdateApkContentProvider.URI, UpdateApkContentProvider.APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun page(build: LinearLayout.() -> Unit): ScrollView {
        val padding = dp(20)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            build()
        }
        return ScrollView(this).apply { addView(content) }
    }

    private fun LinearLayout.addTitle(title: String, subtitle: String) {
        addView(TextView(this@MainActivity).apply {
            text = title
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xff111827.toInt())
        })
        addView(TextView(this@MainActivity).apply {
            text = subtitle
            textSize = 14f
            setTextColor(0xff6b7280.toInt())
            setPadding(0, dp(4), 0, dp(18))
        })
    }

    private fun LinearLayout.addHeader(title: String, subtitle: String) {
        addButton("返回") { renderHome() }
        addTitle(title, subtitle)
    }

    private fun LinearLayout.addEntry(
        title: String,
        summary: String,
        actionText: String,
        onClick: () -> Unit,
    ) {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(0xfff3f4f6.toInt())
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xff111827.toInt())
            })
            addView(TextView(this@MainActivity).apply {
                text = summary
                textSize = 14f
                setTextColor(0xff4b5563.toInt())
                setPadding(0, dp(4), 0, dp(8))
            })
            addButton(actionText, onClick)
        }, spacedParams(dp(10)))
    }

    private fun LinearLayout.addStatus(text: String) {
        addView(TextView(this@MainActivity).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xff0f766e.toInt())
            setPadding(0, dp(8), 0, dp(14))
        })
    }

    private fun LinearLayout.addBodyText(text: String) {
        addView(TextView(this@MainActivity).apply {
            this.text = text
            textSize = 15f
            setTextColor(0xff374151.toInt())
            setLineSpacing(0f, 1.12f)
            setPadding(0, dp(6), 0, dp(14))
        })
    }

    private fun LinearLayout.addRobotSelection(moduleId: String) {
        val selected = NotificationConfigStore.loadModuleRobotIds(this@MainActivity, moduleId)
        val robots = NotificationConfigStore.loadRobots(this@MainActivity)
        if (robots.isEmpty()) {
            addBodyText("暂无通知机器人，请先在通知服务中新增飞书机器人。")
            return
        }
        robots.forEach { robot ->
            addView(CheckBox(this@MainActivity).apply {
                text = "${robot.name}${if (robot.enabled) "" else "（已禁用）"}"
                isChecked = robot.id in selected
                isAllCaps = false
                setOnCheckedChangeListener { _, checked ->
                    val current = NotificationConfigStore.loadModuleRobotIds(this@MainActivity, moduleId)
                    val next = if (checked) current + robot.id else current - robot.id
                    NotificationConfigStore.saveModuleRobotIds(this@MainActivity, moduleId, next)
                    ModuleLogStore.append(this@MainActivity, "通知服务", "短信模块通知目标已更新：${next.size} 个机器人")
                }
            }, spacedParams(dp(8)))
        }
    }

    private fun LinearLayout.addRobotRow(robot: NotificationRobot) {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(0xfff3f4f6.toInt())
            addView(TextView(this@MainActivity).apply {
                text = robot.name
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xff111827.toInt())
            })
            addView(TextView(this@MainActivity).apply {
                text = "${robot.type.name.lowercase()} / ${if (robot.enabled) "启用" else "停用"}"
                textSize = 13f
                setTextColor(0xff4b5563.toInt())
            })
            addButton(if (robot.enabled) "停用" else "启用") {
                NotificationConfigStore.upsertRobot(this@MainActivity, robot.copy(enabled = !robot.enabled))
                renderNotificationService()
            }
            addButton("删除") {
                NotificationConfigStore.deleteRobot(this@MainActivity, robot.id)
                renderNotificationService()
            }
        }, spacedParams(dp(10)))
    }

    private fun LinearLayout.addFeishuRobotForm() {
        val nameInput = EditText(this@MainActivity).apply {
            hint = "机器人名称"
            setSingleLine(true)
        }
        val webhookInput = EditText(this@MainActivity).apply {
            hint = "飞书 Webhook URL"
            setSingleLine(true)
        }
        addBodyText("新增飞书机器人")
        addView(nameInput, spacedParams(dp(8)))
        addView(webhookInput, spacedParams(dp(8)))
        addButton("保存飞书机器人") {
            val name = nameInput.text.toString().trim().ifBlank { "飞书机器人" }
            val webhook = webhookInput.text.toString().trim()
            if (webhook.isBlank()) {
                ModuleLogStore.append(this@MainActivity, "通知服务", "飞书机器人保存失败：webhook 为空")
                renderNotificationService()
                return@addButton
            }
            NotificationConfigStore.upsertRobot(
                this@MainActivity,
                NotificationRobot(
                    id = NotificationConfigStore.createRobotId(),
                    name = name,
                    type = NotificationRobotType.FEISHU,
                    webhookUrl = webhook,
                    enabled = true,
                ),
            )
            ModuleLogStore.append(this@MainActivity, "通知服务", "已新增飞书机器人：$name")
            renderNotificationService()
        }
    }

    private fun LinearLayout.addLogLine(text: String) {
        addView(TextView(this@MainActivity).apply {
            this.text = text
            textSize = 13f
            setTextColor(0xff1f2937.toInt())
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(0xfff9fafb.toInt())
        }, spacedParams(dp(6)))
    }

    private fun LinearLayout.addButton(text: String, onClick: () -> Unit) {
        addView(Button(this@MainActivity).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        }, spacedParams(dp(8)))
    }

    private fun saveTargetProvider(provider: VoiceAssistantProvider) {
        TargetProviderStore.save(this, provider.id)
        ModuleLogStore.append(this, "电源键语音", "目标应用已切换为 ${provider.displayName}")
    }

    private fun selectedProvider(): VoiceAssistantProvider =
        TargetProviderStore.load(this)
            ?.let { VoiceAssistantProviders.byId(it) }
            ?: VoiceAssistantProviders.defaultProvider

    @Suppress("DEPRECATION")
    private fun currentVersionName(): String =
        packageManager.getPackageInfo(packageName, 0).versionName ?: "未知"

    private fun spacedParams(bottomMargin: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.bottomMargin = bottomMargin
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

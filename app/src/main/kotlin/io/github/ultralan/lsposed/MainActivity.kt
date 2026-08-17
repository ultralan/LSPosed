package io.github.ultralan.lsposed

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.ultralan.lsposed.core.ModuleLogStore
import io.github.ultralan.lsposed.core.notification.NotificationConfigStore
import io.github.ultralan.lsposed.core.notification.NotificationRetryScheduler
import io.github.ultralan.lsposed.core.notification.NotificationRetryStore
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
    private val surface = 0xfff9f9ff.toInt()
    private val surfaceContainer = 0xffeef0f8.toInt()
    private val surfaceContainerHigh = 0xffe8eaf2.toInt()
    private val onSurface = 0xff1a1b20.toInt()
    private val onSurfaceVariant = 0xff44474e.toInt()
    private val outline = 0xff74777f.toInt()
    private val primary = 0xff0b57d0.toInt()
    private val onPrimary = 0xffffffff.toInt()
    private val primaryContainer = 0xffd7e2ff.toInt()
    private val onPrimaryContainer = 0xff001a41.toInt()
    private val secondaryContainer = 0xffd6e3ff.toInt()
    private val tertiaryContainer = 0xffbdeff0.toInt()
    private val onTertiaryContainer = 0xff003738.toInt()
    private val errorColor = 0xffba1a1a.toInt()
    private val errorContainer = 0xffffdad6.toInt()

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = surface
        window.navigationBarColor = surface
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        ReadablePreferences.makeTargetProviderPreferencesReadable(this)
        NotificationRetryScheduler.processAsync(this)
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
            addHomeHeader()
            addSectionLabel("核心模块")
            addNavigationItem(
                NavigationItem(
                    iconRes = R.drawable.ic_mic,
                    title = "电源键语音",
                    summary = "当前目标：${selectedProviderLabel()}",
                    iconBackgroundColor = primaryContainer,
                    iconColor = primary,
                    onClick = { renderPowerVoice() },
                ),
            )
            addNavigationItem(
                NavigationItem(
                    iconRes = R.drawable.ic_sms,
                    title = "短信拦截推送",
                    summary = "已连接 ${NotificationConfigStore.loadModuleRobotIds(this@MainActivity, NotificationConfigStore.MODULE_SMS_PUSH).size} 个通道",
                    iconBackgroundColor = tertiaryContainer,
                    iconColor = onTertiaryContainer,
                    onClick = { renderSmsPush() },
                ),
            )
            addSectionLabel("服务与维护")
            addNavigationItem(
                NavigationItem(
                    iconRes = R.drawable.ic_notifications,
                    title = "通知服务",
                    summary = "${NotificationConfigStore.loadRobots(this@MainActivity).count { it.enabled }} 个机器人正在接收模块事件",
                    iconBackgroundColor = secondaryContainer,
                    iconColor = primary,
                    onClick = { renderNotificationService() },
                ),
            )
            addNavigationItem(
                NavigationItem(
                    iconRes = R.drawable.ic_history,
                    title = "运行日志",
                    summary = "${ModuleLogStore.load(this@MainActivity).size} 条记录",
                    iconBackgroundColor = surfaceContainerHigh,
                    iconColor = onSurfaceVariant,
                    onClick = { renderLogs() },
                ),
            )
            addNavigationItem(
                NavigationItem(
                    iconRes = R.drawable.ic_system_update,
                    title = "应用更新",
                    summary = currentVersionName(),
                    iconBackgroundColor = primaryContainer,
                    iconColor = primary,
                    onClick = { renderUpdate() },
                ),
            )
        })
    }

    private fun renderPowerVoice() {
        currentScreen = Screen.POWER_VOICE
        setContentView(page {
            addHeader("电源键语音", "选择目标应用", R.drawable.ic_mic)
            addSettingItem(
                iconRes = R.drawable.ic_settings,
                title = "当前行为",
                summary = "长按电源键：${selectedProviderLabel()}",
                clickable = false,
            )
            addSectionLabel("电源键行为")
            addSelectableItem(
                iconRes = R.drawable.ic_power,
                title = "系统默认",
                summary = "不拦截电源键，保持系统原样",
                selected = TargetProviderStore.isSystemDefault(TargetProviderStore.load(this@MainActivity)),
            ) {
                saveSystemDefaultTarget()
                renderPowerVoice()
            }
            addSectionLabel("目标应用")
            VoiceAssistantProviders.ALL.forEach { provider ->
                addSelectableItem(
                    iconRes = R.drawable.ic_mic,
                    title = provider.displayName,
                    summary = provider.packageName,
                    selected = TargetProviderStore.load(this@MainActivity) == provider.id,
                ) {
                    saveTargetProvider(provider)
                    renderPowerVoice()
                }
            }
        })
    }

    private fun renderSmsPush() {
        currentScreen = Screen.SMS_PUSH
        setContentView(page {
            addHeader("短信拦截推送", "短信 Hook", R.drawable.ic_sms)
            addSettingItem(
                iconRes = R.drawable.ic_sms,
                title = "系统短信服务",
                summary = "作用域：com.android.phone",
                clickable = false,
            )
            addSectionLabel("推送通道")
            addRobotSelection(NotificationConfigStore.MODULE_SMS_PUSH)
        })
    }

    private fun renderNotificationService() {
        currentScreen = Screen.NOTIFICATION
        setContentView(page {
            addHeader("通知服务", "飞书机器人", R.drawable.ic_notifications)
            val robots = NotificationConfigStore.loadRobots(this@MainActivity)
            addSettingItem(
                iconRes = R.drawable.ic_history,
                title = "重试队列",
                summary = "${NotificationRetryStore.pendingCount(this@MainActivity)} 条待发送消息",
                clickable = false,
            )
            addSectionLabel("机器人")
            if (robots.isEmpty()) {
                addEmptyState("暂无通知机器人")
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
            addHeader("运行日志", "模块日志", R.drawable.ic_history)
            val entries = ModuleLogStore.load(this@MainActivity)
            addActionRow(
                ActionItem(R.drawable.ic_refresh, "刷新日志") { renderLogs() },
                ActionItem(R.drawable.ic_delete, "清空日志") {
                ModuleLogStore.clear(this@MainActivity)
                renderLogs()
                },
            )
            addSectionLabel("最近记录")
            if (entries.isEmpty()) {
                addEmptyState("暂无运行日志")
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
            addHeader("应用更新", "GitHub Releases", R.drawable.ic_system_update)
            addSettingItem(
                iconRes = R.drawable.ic_system_update,
                title = "当前版本",
                summary = currentVersionName(),
                clickable = false,
            )
            addSettingItem(
                iconRes = R.drawable.ic_history,
                title = "更新状态",
                summary = updateStatus,
                clickable = false,
            )
            addPrimaryCommand("检查更新", R.drawable.ic_refresh) { checkForUpdate() }
            availableRelease?.let { release ->
                addSectionLabel("可用版本")
                addSettingItem(
                    iconRes = R.drawable.ic_download,
                    title = release.tagName,
                    summary = "已通过 GitHub Releases 获取",
                    clickable = false,
                )
                if (release.releaseNotes.isNotBlank()) {
                    addBodyText(release.releaseNotes)
                }
                addPrimaryCommand("下载并安装 ${release.tagName}", R.drawable.ic_download) {
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

    private data class NavigationItem(
        val iconRes: Int,
        val title: String,
        val summary: String,
        val iconBackgroundColor: Int,
        val iconColor: Int,
        val onClick: () -> Unit,
    )

    private data class ActionItem(
        val iconRes: Int,
        val description: String,
        val onClick: () -> Unit,
    )

    private fun page(build: LinearLayout.() -> Unit): ScrollView {
        val horizontalPadding = dp(20)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(horizontalPadding, dp(24), horizontalPadding, dp(28))
            build()
        }
        return ScrollView(this).apply {
            setBackgroundColor(surface)
            isFillViewport = true
            clipToPadding = false
            setOnApplyWindowInsetsListener { view, insets ->
                val (topInset, bottomInset) = systemBarInsets(insets)
                view.setPadding(0, topInset + dp(8), 0, bottomInset)
                insets
            }
            addView(content)
            requestApplyInsets()
        }
    }

    @Suppress("DEPRECATION")
    private fun systemBarInsets(insets: WindowInsets): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            bars.top to bars.bottom
        } else {
            maxOf(insets.systemWindowInsetTop, insets.displayCutout?.safeInsetTop ?: 0) to insets.systemWindowInsetBottom
        }

    private fun LinearLayout.addHomeHeader() {
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_shield)
                setColorFilter(onPrimary)
                contentDescription = null
                setBackground(roundedShape(primary, 16))
            }, LinearLayout.LayoutParams(dp(52), dp(52)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, 0, 0)
                addView(TextView(this@MainActivity).apply {
                    text = getString(R.string.app_name)
                    textSize = 28f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(onSurface)
                })
                addView(TextView(this@MainActivity).apply {
                    text = getString(R.string.module_console_version, currentVersionName())
                    textSize = 14f
                    setTextColor(onSurfaceVariant)
                    setPadding(0, dp(2), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }, spacedParams(dp(20)))
        addSettingItem(
            iconRes = R.drawable.ic_shield,
            title = "模块状态",
            summary = "配置已加载",
            clickable = false,
        )
    }

    private fun LinearLayout.addHeader(title: String, subtitle: String, iconRes: Int) {
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            addView(ImageButton(this@MainActivity).apply {
                setImageResource(R.drawable.ic_arrow_back)
                setColorFilter(onSurface)
                contentDescription = "返回"
                setBackground(roundedShape(surfaceContainer, 12))
                setOnClickListener { renderHome() }
            }, LinearLayout.LayoutParams(dp(44), dp(44)))
            addView(ImageView(this@MainActivity).apply {
                setImageResource(iconRes)
                setColorFilter(primary)
                contentDescription = null
                setBackground(roundedShape(primaryContainer, 12))
            }, LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                leftMargin = dp(8)
            })
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(TextView(this@MainActivity).apply {
                    text = title
                    textSize = 24f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(onSurface)
                })
                addView(TextView(this@MainActivity).apply {
                    text = subtitle
                    textSize = 13f
                    setTextColor(onSurfaceVariant)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }, spacedParams(dp(20)))
    }

    private fun LinearLayout.addSectionLabel(text: String) {
        addView(TextView(this@MainActivity).apply {
            this.text = text
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(onSurfaceVariant)
            setPadding(dp(4), 0, 0, dp(10))
        })
    }

    private fun LinearLayout.addNavigationItem(item: NavigationItem) {
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            tag = "navigation-item"
            contentDescription = item.title
            isClickable = true
            isFocusable = true
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackground(roundedShape(surfaceContainer, 12))
            setOnClickListener { item.onClick() }
            addView(ImageView(this@MainActivity).apply {
                tag = "navigation-icon"
                setImageResource(item.iconRes)
                setColorFilter(item.iconColor)
                contentDescription = null
                setBackground(roundedShape(item.iconBackgroundColor, 14))
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, dp(8), 0)
                addView(TextView(this@MainActivity).apply {
                    text = item.title
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(onSurface)
                })
                addView(TextView(this@MainActivity).apply {
                    text = item.summary
                    textSize = 13f
                    maxLines = 2
                    setTextColor(onSurfaceVariant)
                    setPadding(0, dp(3), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_chevron_right)
                setColorFilter(onSurfaceVariant)
                contentDescription = null
            }, LinearLayout.LayoutParams(dp(24), dp(24)))
        }, spacedParams(dp(8)))
    }

    private fun LinearLayout.addSettingItem(
        iconRes: Int,
        title: String,
        summary: String,
        selected: Boolean = false,
        clickable: Boolean = true,
        onClick: (() -> Unit)? = null,
    ) {
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = clickable
            isFocusable = clickable
            contentDescription = title
            setBackground(roundedShape(if (selected) primaryContainer else surfaceContainer, 12))
            onClick?.let { setOnClickListener { it() } }
            addView(ImageView(this@MainActivity).apply {
                setImageResource(iconRes)
                setColorFilter(if (selected) onPrimaryContainer else onSurfaceVariant)
                contentDescription = null
                setBackground(roundedShape(if (selected) primaryContainer else surfaceContainerHigh, 12))
            }, LinearLayout.LayoutParams(dp(40), dp(40)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                addView(TextView(this@MainActivity).apply {
                    text = title
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (selected) onPrimaryContainer else onSurface)
                })
                addView(TextView(this@MainActivity).apply {
                    text = summary
                    textSize = 13f
                    maxLines = 2
                    setTextColor(if (selected) onPrimaryContainer else onSurfaceVariant)
                    setPadding(0, dp(3), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (selected || clickable) {
                addView(ImageView(this@MainActivity).apply {
                    setImageResource(if (selected) R.drawable.ic_check else R.drawable.ic_chevron_right)
                    setColorFilter(if (selected) onPrimaryContainer else onSurfaceVariant)
                    contentDescription = null
                }, LinearLayout.LayoutParams(dp(24), dp(24)))
            }
        }, spacedParams(dp(8)))
    }

    private fun LinearLayout.addSelectableItem(
        iconRes: Int,
        title: String,
        summary: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        addSettingItem(iconRes, title, summary, selected, clickable = true, onClick = onClick)
    }

    private fun LinearLayout.addEmptyState(text: String) {
        addView(TextView(this@MainActivity).apply {
            this.text = text
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(onSurfaceVariant)
            setPadding(dp(16), dp(24), dp(16), dp(24))
            setBackground(roundedShape(surfaceContainer, 12))
        }, spacedParams(dp(8)))
    }

    private fun LinearLayout.addActionRow(vararg actions: ActionItem) {
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            actions.forEachIndexed { index, action ->
                addView(ImageButton(this@MainActivity).apply {
                    setImageResource(action.iconRes)
                    setColorFilter(if (index == 0) primary else errorColor)
                    contentDescription = action.description
                    setBackground(roundedShape(if (index == 0) primaryContainer else errorContainer, 12))
                    setOnClickListener { action.onClick() }
                }, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    if (index > 0) leftMargin = dp(8)
                })
            }
        }, spacedParams(dp(16)))
    }

    private fun LinearLayout.addPrimaryCommand(
        label: String,
        iconRes: Int,
        onClick: () -> Unit,
    ) {
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            isClickable = true
            isFocusable = true
            contentDescription = label
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackground(roundedShape(primary, 12))
            setOnClickListener { onClick() }
            addView(ImageView(this@MainActivity).apply {
                setImageResource(iconRes)
                setColorFilter(onPrimary)
                contentDescription = null
            }, LinearLayout.LayoutParams(dp(22), dp(22)))
            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(onPrimary)
                setPadding(dp(8), 0, 0, 0)
            })
        }, spacedParams(dp(12)))
    }

    private fun LinearLayout.addBodyText(text: String) {
        addView(TextView(this@MainActivity).apply {
            this.text = text
            textSize = 15f
            setTextColor(onSurfaceVariant)
            setLineSpacing(0f, 1.2f)
            setPadding(dp(2), dp(2), dp(2), dp(16))
        })
    }

    private fun LinearLayout.addRobotSelection(moduleId: String) {
        val selected = NotificationConfigStore.loadModuleRobotIds(this@MainActivity, moduleId)
        val robots = NotificationConfigStore.loadRobots(this@MainActivity)
        if (robots.isEmpty()) {
            addEmptyState("暂无通知机器人")
            return
        }
        robots.forEach { robot ->
            addSelectableItem(
                iconRes = R.drawable.ic_notifications,
                title = robot.name,
                summary = if (robot.enabled) "已启用" else "已停用",
                selected = robot.id in selected,
            ) {
                val current = NotificationConfigStore.loadModuleRobotIds(this@MainActivity, moduleId)
                val next = if (robot.id in current) current - robot.id else current + robot.id
                NotificationConfigStore.saveModuleRobotIds(this@MainActivity, moduleId, next)
                ModuleLogStore.append(this@MainActivity, "通知服务", "短信模块通知目标已更新：${next.size} 个机器人")
                renderSmsPush()
            }
        }
    }

    private fun LinearLayout.addRobotRow(robot: NotificationRobot) {
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackground(roundedShape(surfaceContainer, 12))
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_notifications)
                setColorFilter(primary)
                contentDescription = null
                setBackground(roundedShape(secondaryContainer, 12))
            }, LinearLayout.LayoutParams(dp(40), dp(40)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                addView(TextView(this@MainActivity).apply {
                    text = robot.name
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(onSurface)
                })
                addView(TextView(this@MainActivity).apply {
                    text = "飞书 / ${if (robot.enabled) "已启用" else "已停用"}"
                    textSize = 13f
                    setTextColor(onSurfaceVariant)
                    setPadding(0, dp(3), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageButton(this@MainActivity).apply {
                setImageResource(R.drawable.ic_power)
                setColorFilter(primary)
                contentDescription = if (robot.enabled) "停用 ${robot.name}" else "启用 ${robot.name}"
                setBackground(roundedShape(secondaryContainer, 12))
                setOnClickListener {
                NotificationConfigStore.upsertRobot(this@MainActivity, robot.copy(enabled = !robot.enabled))
                renderNotificationService()
                }
            }, LinearLayout.LayoutParams(dp(44), dp(44)))
            addView(ImageButton(this@MainActivity).apply {
                setImageResource(R.drawable.ic_delete)
                setColorFilter(errorColor)
                contentDescription = "删除 ${robot.name}"
                setBackground(roundedShape(errorContainer, 12))
                setOnClickListener {
                NotificationConfigStore.deleteRobot(this@MainActivity, robot.id)
                renderNotificationService()
                }
            }, LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                leftMargin = dp(8)
            })
        }, spacedParams(dp(8)))
    }

    private fun LinearLayout.addFeishuRobotForm() {
        val nameInput = EditText(this@MainActivity).apply {
            hint = "机器人名称"
            setSingleLine(true)
            textSize = 15f
            setTextColor(onSurface)
            setHintTextColor(onSurfaceVariant)
            setPadding(dp(14), 0, dp(14), 0)
            setBackground(roundedShape(surfaceContainerHigh, 12))
        }
        val webhookInput = EditText(this@MainActivity).apply {
            hint = "飞书 Webhook URL"
            setSingleLine(true)
            textSize = 15f
            setTextColor(onSurface)
            setHintTextColor(onSurfaceVariant)
            setPadding(dp(14), 0, dp(14), 0)
            setBackground(roundedShape(surfaceContainerHigh, 12))
        }
        addSectionLabel("新增机器人")
        addView(nameInput, inputParams())
        addView(webhookInput, inputParams())
        addPrimaryCommand("添加飞书机器人", R.drawable.ic_add) {
            val name = nameInput.text.toString().trim().ifBlank { "飞书机器人" }
            val webhook = webhookInput.text.toString().trim()
            if (webhook.isBlank()) {
                ModuleLogStore.append(this@MainActivity, "通知服务", "飞书机器人保存失败：webhook 为空")
                renderNotificationService()
                return@addPrimaryCommand
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
            setTextColor(onSurfaceVariant)
            setLineSpacing(0f, 1.16f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackground(roundedShape(surfaceContainer, 8))
        }, spacedParams(dp(6)))
    }

    private fun saveTargetProvider(provider: VoiceAssistantProvider) {
        TargetProviderStore.save(this, provider.id)
        ModuleLogStore.append(this, "电源键语音", "目标应用已切换为 ${provider.displayName}")
    }

    private fun saveSystemDefaultTarget() {
        TargetProviderStore.save(this, TargetProviderStore.SYSTEM_DEFAULT_ID)
        ModuleLogStore.append(this, "电源键语音", "已切换为系统默认，不再映射电源键")
    }

    private fun selectedProviderLabel(): String =
        if (TargetProviderStore.isSystemDefault(TargetProviderStore.load(this))) "系统默认（不映射）"
        else selectedProvider().displayName

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

    private fun inputParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56),
        ).apply {
            bottomMargin = dp(8)
        }

    private fun roundedShape(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

package io.github.ultralan.lsposed

import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import io.github.ultralan.lsposed.core.ModuleLogStore
import io.github.ultralan.lsposed.core.notification.NotificationConfigStore
import io.github.ultralan.lsposed.features.powervoice.PowerVoiceConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class MainActivityTest {
    @Test
    fun `home page shows module and log subpage entries`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        val text = activity.window.decorView.visibleText()

        assertTrue(text.contains("电源键语音"))
        assertTrue(text.contains("短信拦截推送"))
        assertTrue(text.contains("通知服务"))
        assertTrue(text.contains("运行日志"))
    }

    @Test
    fun `clicking power voice entry opens provider config subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        activity.window.decorView.findButtons()
            .first { it.text.contains("电源键语音") }
            .performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("选择目标应用"))
        assertTrue(text.contains("当前目标"))
    }

    @Test
    fun `clicking provider button in power voice subpage saves target provider`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()
        activity.window.decorView.findButtons()
            .first { it.text.contains("电源键语音") }
            .performClick()
        val kimiButton = activity.window.decorView.findButtons()
            .first { it.text.contains("Kimi") }

        kimiButton.performClick()

        val selected = activity.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, 0)
            .getString(PowerVoiceConfig.PREF_KEY_TARGET_PROVIDER_ID, null)
        assertEquals("kimi", selected)
    }

    @Test
    fun `clicking sms entry opens sms config subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        activity.window.decorView.findButtons()
            .first { it.text.contains("短信拦截推送") }
            .performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("短信 Hook"))
        assertTrue(text.contains("com.android.phone"))
        assertTrue(text.contains("请先在通知服务中新增飞书机器人"))
    }

    @Test
    fun `sms subpage robot checkbox updates module robot selection`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()
        NotificationConfigStore.saveRobots(
            activity,
            listOf(
                io.github.ultralan.lsposed.core.notification.NotificationRobot(
                    id = "test_feishu",
                    name = "测试飞书机器人",
                    type = io.github.ultralan.lsposed.core.notification.NotificationRobotType.FEISHU,
                    webhookUrl = "https://example.com/webhook",
                    enabled = true,
                ),
            ),
        )

        activity.window.decorView.findButtons()
            .first { it.text.contains("短信拦截推送") }
            .performClick()
        activity.window.decorView.findCheckBoxes()
            .first { it.text.contains("测试飞书机器人") }
            .performClick()

        assertEquals(
            setOf("test_feishu"),
            NotificationConfigStore.loadModuleRobotIds(activity, NotificationConfigStore.MODULE_SMS_PUSH),
        )
    }

    @Test
    fun `clicking notification entry opens robot config subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        activity.window.decorView.findButtons()
            .first { it.text.contains("通知服务") }
            .performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("飞书机器人"))
        assertTrue(text.contains("暂无通知机器人"))
        assertTrue(text.contains("新增飞书机器人"))
    }

    @Test
    fun `clicking log entry opens module log subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()
        ModuleLogStore.append(activity, "测试", "短信 Hook 捕获：from=10086 body=验证码 123456")

        activity.window.decorView.findButtons()
            .first { it.text.contains("运行日志") }
            .performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("模块日志"))
        assertTrue(text.contains("验证码 123456"))
    }

    private fun android.view.View.findButtons(): List<Button> {
        val found = mutableListOf<Button>()
        if (this is Button) found += this
        if (this is android.view.ViewGroup) {
            for (index in 0 until childCount) {
                found += getChildAt(index).findButtons()
            }
        }
        return found
    }

    private fun android.view.View.findCheckBoxes(): List<CheckBox> {
        val found = mutableListOf<CheckBox>()
        if (this is CheckBox) found += this
        if (this is android.view.ViewGroup) {
            for (index in 0 until childCount) {
                found += getChildAt(index).findCheckBoxes()
            }
        }
        return found
    }

    private fun android.view.View.visibleText(): String {
        val found = mutableListOf<String>()
        collectText(found)
        return found.joinToString("\n")
    }

    private fun android.view.View.collectText(found: MutableList<String>) {
        if (this is TextView) found += text.toString()
        if (this is android.view.ViewGroup) {
            for (index in 0 until childCount) {
                getChildAt(index).collectText(found)
            }
        }
    }
}

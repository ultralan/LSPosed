package io.github.ultralan.lsposed

import android.widget.TextView
import org.junit.runner.RunWith
import org.junit.Before
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import io.github.ultralan.lsposed.core.ModuleLogStore
import io.github.ultralan.lsposed.core.notification.NotificationConfigStore
import io.github.ultralan.lsposed.features.powervoice.PowerVoiceConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class MainActivityTest {
    @Before
    fun resetNotificationConfig() {
        NotificationConfigStore.clear(RuntimeEnvironment.getApplication())
    }

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
        assertTrue(text.contains("应用更新"))
    }

    @Test
    fun `home page groups controls into module and system sections`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        val text = activity.window.decorView.visibleText()

        assertTrue(text.contains("核心模块"))
        assertTrue(text.contains("服务与维护"))
        assertTrue(text.contains("模块状态"))
    }

    @Test
    fun `home page renders five icon navigation items`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        assertEquals(
            5,
            activity.window.decorView.findViewsWithTag("navigation-item").size,
        )
        assertEquals(5, activity.window.decorView.findViewsWithTag("navigation-icon").size)
    }

    @Test
    fun `clicking update entry opens update subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        activity.window.decorView.findNavigationItem("应用更新").performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("当前版本"))
        assertTrue(text.contains("检查更新"))
    }

    @Test
    fun `clicking power voice entry opens provider config subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        activity.window.decorView.findNavigationItem("电源键语音").performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("选择目标应用"))
        assertTrue(text.contains("电源键行为"))
    }

    @Test
    fun `clicking provider button in power voice subpage saves target provider`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()
        activity.window.decorView.findNavigationItem("电源键语音").performClick()
        val kimiButton = activity.window.decorView.findViewWithContentDescription("Kimi")

        kimiButton.performClick()

        val selected = activity.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, 0)
            .getString(PowerVoiceConfig.PREF_KEY_TARGET_PROVIDER_ID, null)
        assertEquals("kimi", selected)
    }

    @Test
    fun `clicking system default saves an unmapped power key target`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()
        activity.window.decorView.findNavigationItem("电源键语音").performClick()

        activity.window.decorView.findViewWithContentDescription("系统默认").performClick()

        val selected = activity.getSharedPreferences(PowerVoiceConfig.PREFS_NAME, 0)
            .getString(PowerVoiceConfig.PREF_KEY_TARGET_PROVIDER_ID, null)
        assertEquals("system_default", selected)
        assertTrue(activity.window.decorView.visibleText().contains("不拦截电源键"))
    }

    @Test
    fun `clicking sms entry opens sms config subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        activity.window.decorView.findNavigationItem("短信拦截推送").performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("短信 Hook"))
        assertTrue(text.contains("com.android.phone"))
        assertTrue(text.contains("默认飞书机器人"))
    }

    @Test
    fun `sms subpage robot checkbox updates module robot selection`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()
        activity.window.decorView.findNavigationItem("短信拦截推送").performClick()
        activity.window.decorView.findViewWithContentDescription("默认飞书机器人").performClick()

        assertEquals(
            emptySet(),
            NotificationConfigStore.loadModuleRobotIds(activity, NotificationConfigStore.MODULE_SMS_PUSH),
        )
    }

    @Test
    fun `clicking notification entry opens robot config subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        activity.window.decorView.findNavigationItem("通知服务").performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("飞书机器人"))
        assertTrue(text.contains("默认飞书机器人"))
        assertTrue(text.contains("新增机器人"))
    }

    @Test
    fun `clicking log entry opens module log subpage`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()
        ModuleLogStore.append(activity, "测试", "短信 Hook 捕获：from=10086 body=验证码 123456")

        activity.window.decorView.findNavigationItem("运行日志").performClick()

        val text = activity.window.decorView.visibleText()
        assertTrue(text.contains("模块日志"))
        assertTrue(text.contains("验证码 123456"))
    }

    private fun android.view.View.findViewWithContentDescription(description: String): android.view.View {
        if (contentDescription == description) return this
        if (this is android.view.ViewGroup) {
            for (index in 0 until childCount) {
                runCatching { return getChildAt(index).findViewWithContentDescription(description) }
            }
        }
        throw NoSuchElementException("找不到 $description")
    }

    private fun android.view.View.findViewsWithTag(tag: String): List<android.view.View> {
        val found = mutableListOf<android.view.View>()
        if (this.tag == tag) found += this
        if (this is android.view.ViewGroup) {
            for (index in 0 until childCount) {
                found += getChildAt(index).findViewsWithTag(tag)
            }
        }
        return found
    }

    private fun android.view.View.findNavigationItem(title: String): android.view.View =
        findViewsWithTag("navigation-item")
            .first { it.contentDescription?.contains(title) == true }

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

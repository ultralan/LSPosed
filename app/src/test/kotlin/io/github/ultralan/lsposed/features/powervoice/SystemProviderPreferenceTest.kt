package io.github.ultralan.lsposed.features.powervoice

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class SystemProviderPreferenceTest {
    @Test
    fun `system selection uses provider id queried from content provider`() {
        val context = RuntimeEnvironment.getApplication()
        TargetProviderStore.save(context, "tongyi")

        val selected = ProviderSelector.select(
            preferredProviderId = TargetProviderStore.query(context),
            installedPackageNames = setOf(PowerVoiceConfig.DOUBAO_PACKAGE, "com.aliyun.tongyi"),
        )

        assertEquals("tongyi", selected.id)
    }
}

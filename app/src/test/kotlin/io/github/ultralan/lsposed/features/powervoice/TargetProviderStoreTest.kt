package io.github.ultralan.lsposed.features.powervoice

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class TargetProviderStoreTest {
    @Test
    fun `query reads target provider through content provider`() {
        val context = RuntimeEnvironment.getApplication()

        TargetProviderStore.save(context, "tongyi")

        assertEquals("tongyi", TargetProviderStore.query(context))
    }

    @Test
    fun `system default selection persists as an explicit provider marker`() {
        val context = RuntimeEnvironment.getApplication()

        TargetProviderStore.save(context, TargetProviderStore.SYSTEM_DEFAULT_ID)

        assertEquals(TargetProviderStore.SYSTEM_DEFAULT_ID, TargetProviderStore.query(context))
        assertEquals(true, TargetProviderStore.isSystemDefault(TargetProviderStore.load(context)))
    }
}

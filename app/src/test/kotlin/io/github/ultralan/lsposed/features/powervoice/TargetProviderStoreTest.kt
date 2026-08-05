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
}

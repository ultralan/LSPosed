package io.github.ultralan.lsposed.core.update

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class GitHubReleaseParserTest {
    @Test
    fun `parses LSPosed apk asset and digest from latest release`() {
        val release = GitHubReleaseParser.parse(
            """
            {
              "tag_name": "v0.1.1",
              "body": "新增应用内更新",
              "assets": [
                {
                  "name": "LSPosed.apk",
                  "browser_download_url": "https://github.com/ultralan/LSPosed/releases/download/v0.1.1/LSPosed.apk",
                  "digest": "sha256:abc123"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("v0.1.1", release.tagName)
        assertEquals("新增应用内更新", release.releaseNotes)
        assertEquals(
            "https://github.com/ultralan/LSPosed/releases/download/v0.1.1/LSPosed.apk",
            release.apkUrl,
        )
        assertEquals("abc123", release.sha256)
    }
}

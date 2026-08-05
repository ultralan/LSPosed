package io.github.ultralan.lsposed.core.update

import org.json.JSONObject

data class GitHubRelease(
    val tagName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val sha256: String,
)

object GitHubReleaseParser {
    fun parse(json: String): GitHubRelease {
        val root = JSONObject(json)
        val assets = root.getJSONArray("assets")
        val apkAsset = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name") == "LSPosed.apk" }
            ?: error("最新 Release 中未找到 LSPosed.apk")
        val digest = apkAsset.optString("digest")
            .removePrefix("sha256:")
            .takeIf { it.isNotBlank() }
            ?: error("最新 Release 未提供 APK SHA-256")
        return GitHubRelease(
            tagName = root.getString("tag_name"),
            releaseNotes = root.optString("body"),
            apkUrl = apkAsset.getString("browser_download_url"),
            sha256 = digest,
        )
    }
}

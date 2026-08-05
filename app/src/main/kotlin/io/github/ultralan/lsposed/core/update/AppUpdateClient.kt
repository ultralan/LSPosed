package io.github.ultralan.lsposed.core.update

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateClient {
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/ultralan/LSPosed/releases/latest"

    fun fetchLatestRelease(): GitHubRelease {
        val connection = open(LATEST_RELEASE_API)
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            GitHubReleaseParser.parse(reader.readText())
        }.also {
            connection.disconnect()
        }
    }

    fun downloadAndVerify(context: Context, release: GitHubRelease): File {
        val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
        val temporaryFile = File(updateDirectory, "LSPosed.download")
        val apkFile = File(updateDirectory, "LSPosed.apk")
        val connection = open(release.apkUrl)
        connection.inputStream.use { input ->
            temporaryFile.outputStream().use { output -> input.copyTo(output) }
        }
        connection.disconnect()
        UpdateApkVerifier.verify(context, temporaryFile, release.sha256)
        if (apkFile.exists() && !apkFile.delete()) error("无法替换旧更新包")
        if (!temporaryFile.renameTo(apkFile)) error("无法保存更新包")
        return apkFile
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "LSPosed-Android-Updater")
            if (responseCode !in 200..299) {
                error("请求失败：HTTP $responseCode")
            }
        }
}

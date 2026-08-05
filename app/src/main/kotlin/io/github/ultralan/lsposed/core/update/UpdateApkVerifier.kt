package io.github.ultralan.lsposed.core.update

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import java.security.MessageDigest

object UpdateApkVerifier {
    fun verify(context: Context, apkFile: File, expectedSha256: String) {
        require(sha256(apkFile).equals(expectedSha256, ignoreCase = true)) {
            "APK SHA-256 校验失败"
        }
        val archiveInfo = context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES,
        ) ?: error("无法读取 APK 包信息")
        require(archiveInfo.packageName == context.packageName) {
            "APK 包名不匹配：${archiveInfo.packageName}"
        }
        val installedInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
        val installedSigners = installedInfo.signingInfo?.apkContentsSigners
            ?.map { signer -> digest(signer.toByteArray()) }
            ?.toSet()
            ?: emptySet()
        val archiveSigners = archiveInfo.signingInfo?.apkContentsSigners
            ?.map { signer -> digest(signer.toByteArray()) }
            ?.toSet()
            ?: emptySet()
        require(installedSigners.isNotEmpty() && installedSigners == archiveSigners) {
            "APK 签名与当前应用不一致"
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest(digest.digest())
    }

    private fun digest(bytes: ByteArray): String =
        bytes.joinToString("") { byte -> "%02x".format(byte) }
}

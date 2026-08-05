package io.github.ultralan.lsposed.features.powervoice

import android.content.Context
import android.system.Os
import java.io.File

object ReadablePreferences {
    private const val SEARCHABLE_DIRECTORY_MODE = 0b111_001_001
    private const val READABLE_FILE_MODE = 0b110_100_100

    fun makeTargetProviderPreferencesReadable(context: Context) {
        val dataDir = context.applicationInfo.dataDir?.let(::File) ?: return
        makeTargetProviderPreferencesReadable(dataDir)
    }

    internal fun makeTargetProviderPreferencesReadable(
        dataDir: File,
        chmod: (File, Int) -> Unit = ::chmod,
    ) {
        val sharedPrefsDir = File(dataDir, "shared_prefs")
        makeSearchableDirectory(dataDir, chmod)
        makeSearchableDirectory(sharedPrefsDir, chmod)
        makeReadableFile(File(sharedPrefsDir, "${PowerVoiceConfig.PREFS_NAME}.xml"), chmod)
    }

    fun makeReadable(
        file: File,
        chmod: (File, Int) -> Unit = ::chmod,
    ): Boolean {
        if (!file.exists()) return false
        return if (file.isDirectory) {
            makeSearchableDirectory(file, chmod)
        } else {
            makeReadableFile(file, chmod)
        }
    }

    private fun makeSearchableDirectory(
        directory: File,
        chmod: (File, Int) -> Unit,
    ): Boolean {
        if (!directory.exists() || !directory.isDirectory) return false
        return applyMode(directory, SEARCHABLE_DIRECTORY_MODE, chmod)
    }

    private fun makeReadableFile(
        file: File,
        chmod: (File, Int) -> Unit,
    ): Boolean {
        if (!file.exists() || !file.isFile) return false
        return applyMode(file, READABLE_FILE_MODE, chmod)
    }

    private fun applyMode(
        file: File,
        mode: Int,
        chmod: (File, Int) -> Unit,
    ): Boolean {
        return runCatching {
            chmod(file, mode)
            true
        }.getOrDefault(false)
    }

    private fun chmod(file: File, mode: Int) {
        Os.chmod(file.absolutePath, mode)
    }
}

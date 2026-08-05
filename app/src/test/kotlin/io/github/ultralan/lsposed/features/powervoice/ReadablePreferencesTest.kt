package io.github.ultralan.lsposed.features.powervoice

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadablePreferencesTest {
    @Test
    fun `make readable ignores missing files`() {
        val missing = File("build/tmp/missing-readable-pref")
        val chmodCalls = mutableListOf<Pair<File, Int>>()

        assertFalse(ReadablePreferences.makeReadable(missing) { file, mode ->
            chmodCalls += file to mode
        })
        assertTrue(chmodCalls.isEmpty())
    }

    @Test
    fun `make readable applies shared-preference file mode`() {
        val file = File("build/tmp/readable-pref-test.xml").apply {
            parentFile?.mkdirs()
            writeText("<map />")
        }
        val chmodCalls = mutableListOf<Pair<File, Int>>()

        assertTrue(ReadablePreferences.makeReadable(file) { changedFile, mode ->
            chmodCalls += changedFile to mode
        })
        assertEquals(listOf(file to 0b110_100_100), chmodCalls)
    }

    @Test
    fun `make readable applies searchable directory mode`() {
        val directory = File("build/tmp/readable-pref-dir").apply {
            mkdirs()
        }
        val chmodCalls = mutableListOf<Pair<File, Int>>()

        assertTrue(ReadablePreferences.makeReadable(directory) { changedFile, mode ->
            chmodCalls += changedFile to mode
        })
        assertEquals(listOf(directory to 0b111_001_001), chmodCalls)
    }

    @Test
    fun `make target provider preferences readable applies modes across preference path`() {
        val dataDir = File("build/tmp/readable-pref-data").apply {
            deleteRecursively()
            mkdirs()
        }
        val sharedPrefsDir = File(dataDir, "shared_prefs").apply { mkdirs() }
        val prefsFile = File(sharedPrefsDir, "${PowerVoiceConfig.PREFS_NAME}.xml").apply {
            writeText("<map />")
        }
        val chmodCalls = mutableListOf<Pair<File, Int>>()

        ReadablePreferences.makeTargetProviderPreferencesReadable(dataDir) { file, mode ->
            chmodCalls += file to mode
        }

        assertEquals(
            listOf(
                dataDir to 0b111_001_001,
                sharedPrefsDir to 0b111_001_001,
                prefsFile to 0b110_100_100,
            ),
            chmodCalls,
        )
    }
}

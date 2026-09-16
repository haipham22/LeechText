package dev.haipham22.leechtext.plugin.util

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.LeechJson
import okio.Path.Companion.toPath
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginPersistenceTest {
    private lateinit var tmp: File
    private val pluginPersistence = PluginPersistence(platformEngineLogger())

    @BeforeTest
    fun setUp() {
        tmp = createTempDirectory("leech-plugin-persist").toFile()
        EnginePaths.init(tmp.path.toPath())
    }

    @AfterTest
    fun tearDown() {
        tmp.deleteRecursively()
    }

    private fun validPlugin() = PluginEntity(
        uuid = "s1",
        name = "Sample",
        version = 1.5,
        regex = "sample.example.com",
        chapGetter = "function c(){}",
    )

    @Test
    fun validateNullReturnsFalse() {
        assertFalse(pluginPersistence.validate(null))
    }

    @Test
    fun validateMissingNameReturnsFalse() {
        assertFalse(pluginPersistence.validate(PluginEntity(regex = "a.com", chapGetter = "x")))
    }

    @Test
    fun validateMissingRegexReturnsFalse() {
        assertFalse(pluginPersistence.validate(PluginEntity(name = "N", chapGetter = "x")))
    }

    @Test
    fun validateNonCompilableRegexReturnsFalse() {
        assertFalse(pluginPersistence.validate(PluginEntity(name = "N", regex = "[", chapGetter = "x")))
    }

    @Test
    fun validateMissingScriptGetterReturnsFalse() {
        assertFalse(pluginPersistence.validate(PluginEntity(name = "N", regex = "a.com")))
    }

    @Test
    fun validateTocGetterOnlyIsValid() {
        assertTrue(pluginPersistence.validate(PluginEntity(name = "N", regex = "a.com", tocGetter = "t")))
    }

    @Test
    fun validateCompletePluginReturnsTrue() {
        assertTrue(pluginPersistence.validate(validPlugin()))
    }

    @Test
    fun saveAtomicWritesPluginFileRoundTripWithoutLeftoverTmp() {
        val path = pluginPersistence.saveAtomic(validPlugin())

        assertTrue(path.toString().endsWith("s1.plugin"), "file đặt theo uuid")
        assertTrue(File(path.toFile().absolutePath).exists())
        assertFalse(
            File(File(EnginePaths.dataDir.toFile(), "tools/plugins"), "s1.plugin.tmp").exists(),
            "temp file phải được move đi",
        )

        val loaded = LeechJson.decodeFromString<PluginEntity>(path.toFile().readText())
        assertEquals("Sample", loaded.name)
        assertEquals(1.5, loaded.version)
        assertEquals("sample.example.com", loaded.regex)
        assertEquals("function c(){}", loaded.chapGetter)

        // Save lại cùng uuid — overwrite không lỗi (REPLACE_EXISTING)
        pluginPersistence.saveAtomic(validPlugin().copy(version = 2.0))
        val reloaded = LeechJson.decodeFromString<PluginEntity>(path.toFile().readText())
        assertEquals(2.0, reloaded.version)
    }

    @Test
    fun getPluginsDirectoryIsUnderDataDirToolsPlugins() {
        val dir = pluginPersistence.getPluginsDirectory()
        assertEquals(
            File(EnginePaths.dataDir.toFile(), "tools/plugins").absolutePath,
            dir.toFile().absolutePath,
        )
    }
}

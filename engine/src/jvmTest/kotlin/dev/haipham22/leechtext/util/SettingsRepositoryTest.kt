package dev.haipham22.leechtext.util

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val USER_HOME_KEY = "user.home"
private const val TEST_WORKSPACE = "/tmp/ws"
private const val SETTING_FILE = "tools/setting.json"

class SettingsRepositoryTest {
    private lateinit var originalHome: String
    private lateinit var tmpHome: File

    @BeforeTest
    fun setUp() {
        originalHome = System.getProperty(USER_HOME_KEY)
        tmpHome = createTempDirectory("leech-settings").toFile()
        System.setProperty(USER_HOME_KEY, tmpHome.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        System.setProperty(USER_HOME_KEY, originalHome)
    }

    @Test
    fun defaultsLoadsResourceDefaults() {
        val s = SettingsRepository.defaults()

        assertEquals(5, s.maxConn)
        assertEquals(90000, s.timeout)
        assertEquals(3, s.reConn)
        assertEquals(10, s.delay)
        assertTrue(s.userAgent.contains("iPhone"))
        // default luôn áp syntax kể cả checked=false (semantics isDefault bản gốc)
        assertTrue(s.htmlSyntax.contains("<html"))
        assertTrue(s.txtSyntax.contains("[PARAGRAPH]"))
        assertTrue(s.cssSyntax.contains(".header"))
        assertTrue(s.trash.isNotEmpty())
        assertEquals("#263238", s.themeColor)
    }

    @Test
    fun saveThenLoadRoundTripsSettingsFile() {
        val s =
            SettingsRepository.defaults().copy(
                maxConn = 7,
                timeout = 12345,
                workPath = TEST_WORKSPACE,
                dropcapsEnabled = true,
                dropSyntax = "<span class=\"drop\">[DROP]</span>",
                readerAutoScrollSpeed = 7,
                readerScrollMode = "paged",
            )

        java.nio.file.Files.createDirectories(java.nio.file.Path.of(TEST_WORKSPACE)) // workspace hợp lệ phải tồn tại (semantics mới: folder mất = reset)
        SettingsRepository.save(s)
        val saved = File(EnginePaths.dataDir.toFile(), SETTING_FILE)
        assertTrue(saved.exists(), "setting.json phải được ghi ra dataDir/tools")

        val loaded = SettingsRepository.load()
        assertEquals(7, loaded.maxConn)
        assertEquals(12345, loaded.timeout)
        assertEquals(TEST_WORKSPACE, loaded.workPath)
        assertEquals(true, loaded.dropcapsEnabled)
        assertEquals("<span class=\"drop\">[DROP]</span>", loaded.dropSyntax)
        assertEquals(7, loaded.readerAutoScrollSpeed)
        assertEquals("paged", loaded.readerScrollMode)
    }

    @Test
    fun loadWithUncheckedUserStyleKeepsDefaultSyntax() {
        // user file chỉ đổi connection, style giữ checked=false → syntax vẫn là default
        SettingsRepository.save(
            SettingsRepository.defaults().copy(htmlChecked = false, htmlSyntax = "<b>user</b>"),
        )
        val loaded = SettingsRepository.load()
        assertTrue(loaded.htmlSyntax.contains("<html"), "unchecked syntax không đè default")
    }

    @Test
    fun loadFallsBackToDefaultsOnBrokenJson() {
        val f = File(EnginePaths.dataDir.toFile(), SETTING_FILE)
        f.parentFile.mkdirs()
        f.writeText("{ not json")

        val s = SettingsRepository.load()
        assertEquals(5, s.maxConn)
        assertEquals(90000, s.timeout)
    }

    @Test
    fun savedJsonKeepsLegacyFieldNames() {
        SettingsRepository.save(SettingsRepository.defaults())
        val json = File(EnginePaths.dataDir.toFile(), SETTING_FILE).readText()
        assertTrue(json.contains("\"num_conn\""))
        assertTrue(json.contains("\"user_agent\""))
        assertTrue(json.contains("\"theme_color\""))
    }
}

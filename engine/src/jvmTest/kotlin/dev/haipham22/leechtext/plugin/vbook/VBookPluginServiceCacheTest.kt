package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.vbook.model.VBookExtensionEntity
import dev.haipham22.leechtext.util.EnginePaths
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

private const val USER_HOME_KEY = "user.home"

/**
 * VBookPluginService — chỉ phần pure disk (extension cache, clearCache) +
 * early-return không đụng mạng. user.home trỏ temp như SettingsRepositoryTest.
 */
class VBookPluginServiceCacheTest {
    private lateinit var originalHome: String
    private val service = VBookPluginService(platformEngineLogger(), pluginManager = PluginManager(platformEngineLogger()))

    @BeforeTest
    fun setUp() {
        originalHome = System.getProperty(USER_HOME_KEY)
        val tmpHome = createTempDirectory("leech-vbook").toFile()
        System.setProperty(USER_HOME_KEY, tmpHome.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        System.setProperty(USER_HOME_KEY, originalHome)
    }

    @Test
    fun extensionCacheRoundTripViaDisk() {
        assertEquals(emptyList(), service.loadCachedExtensions(), "chưa có cache → rỗng")

        service.saveCachedExtensions(
            listOf(
                VBookExtensionEntity(
                    name = "truyenfull",
                    path = "https://example.com/plugin.zip",
                    version = 1,
                    source = "https://truyenfull.vn",
                ),
            ),
        )

        val loaded = service.loadCachedExtensions()
        assertEquals(1, loaded.size)
        assertEquals("truyenfull", loaded[0].name)
        assertEquals("https://truyenfull.vn", loaded[0].source)
    }

    @Test
    fun corruptCacheJsonReturnsEmptyListWithoutThrowing() {
        val f = File(EnginePaths.dataDir.toFile(), "tools/extensions-cache.json")
        f.parentFile?.mkdirs()
        f.writeText("{not json")
        assertEquals(emptyList(), service.loadCachedExtensions())
    }

    @Test
    fun clearCacheDeletesVbookCacheAndNoThrowWhenDirMissing() {
        val cacheDir = File(EnginePaths.dataDir.toFile(), "tools/plugins/vbook-cache")
        File(cacheDir, "sub").mkdirs()
        File(cacheDir, "sub/a.zip").writeText("x")
        File(cacheDir, "b.zip").writeText("y")

        service.clearCache()

        assertFalse(File(cacheDir, "sub/a.zip").exists())
        assertFalse(File(cacheDir, "b.zip").exists())
        // dir thiếu / rỗng — không throw
        service.clearCache()
    }

    @Test
    fun findAndInstallByUrlEmptyUrlReturnsNullWithoutNetwork() = kotlinx.coroutines.runBlocking {
        assertNull(service.findAndInstallByUrl(null))
        assertNull(service.findAndInstallByUrl(""))
    }
}

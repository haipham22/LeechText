package dev.haipham22.leechtext.plugin

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.util.EnginePaths
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val REPO_URL = "https://repo.example.com/plugins.json"

/**
 * Test RepositoryManager phần pure local: add/remove/setEnabled/persist — không network.
 * Instance load 1 lần lúc tạo nên mỗi test tạo instance mới trên temp dir mới
 * (EnginePaths.init trong setUp phải chạy trước khi tạo).
 */
class RepositoryManagerTest {
    private lateinit var tmp: File
    private lateinit var repositoryManager: RepositoryManager
    private val repoFile: File get() = File(EnginePaths.dataDir.toFile(), "tools/repository.json")

    @BeforeTest
    fun setUp() {
        tmp = createTempDirectory("leech-repo-mgr").toFile()
        EnginePaths.init(tmp.path.toPath())
        // Load từ temp trống — temp dir mới = list rỗng
        repositoryManager = RepositoryManager(platformEngineLogger())
        assertTrue(repositoryManager.repositoryList().isEmpty(), "phải reset state trước test")
    }

    @AfterTest
    fun tearDown() {
        tmp.deleteRecursively()
    }

    @Test
    fun hasRepoSettingFalseWhenNoFile() {
        assertFalse(repoFile.exists())
        assertFalse(repositoryManager.hasRepoSetting())
    }

    @Test
    fun addValidLinkPersistsRepositoryJson() {
        assertTrue(repositoryManager.add(REPO_URL))

        val list = repositoryManager.repositoryList()
        assertEquals(1, list.size)
        assertEquals(REPO_URL, list.single().link)
        assertTrue(list.single().isEnabled, "repo add mặc định enabled")

        assertTrue(repoFile.exists(), "add phải persist repository.json")
        assertTrue(repoFile.readText().contains("repo.example.com"))
        assertTrue(repositoryManager.hasRepoSetting())
    }

    @Test
    fun addDuplicateLinkReturnsFalse() {
        repositoryManager.add(REPO_URL)
        assertFalse(repositoryManager.add(REPO_URL))
        assertEquals(1, repositoryManager.repositoryList().size)
    }

    @Test
    fun addInvalidLinkReturnsFalse() {
        assertFalse(repositoryManager.add("ftp://repo.example.com/x"))
        assertFalse(repositoryManager.add("khong-phai-link"))
        assertFalse(repositoryManager.add("   "))
        assertTrue(repositoryManager.repositoryList().isEmpty())
    }

    @Test
    fun removeDeletesRepoAndPersists() {
        repositoryManager.add(REPO_URL)
        val entity = repositoryManager.repositoryList().single()

        repositoryManager.remove(entity)

        assertTrue(repositoryManager.repositoryList().isEmpty())
        assertFalse(repoFile.readText().contains("repo.example.com"))
    }

    @Test
    fun setEnabledFalseWritesEnabledFalse() {
        repositoryManager.add(REPO_URL)
        val entity = repositoryManager.repositoryList().single()

        repositoryManager.setEnabled(entity, false)

        assertFalse(repositoryManager.repositoryList().single().isEnabled)
        assertTrue(repoFile.readText().contains("\"enabled\":false"))

        // Set lại true
        repositoryManager.setEnabled(entity, true)
        assertTrue(repositoryManager.repositoryList().single().isEnabled)
    }

    @Test
    fun checkUpdateNoOpWhenNoRepo() {
        // Pure guard: list rỗng → return ngay, không đụng network
        assertTrue(repositoryManager.repositoryList().isEmpty())
        val pluginUpdate = PluginUpdate(platformEngineLogger(), PluginManager(platformEngineLogger()), repositoryManager)
        runBlocking { pluginUpdate.checkUpdate() } // hoàn thành không exception
    }
}

package dev.haipham22.leechtext.plugin

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.util.EnginePaths
import okio.Path.Companion.toPath
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val TRUYENCV_REGEX = "truyencv.com"
private const val TRUYENFULL_LIVE_SOURCE = "https://truyenfull.live"

/**
 * Test PluginManager: add/get/remove/dedupe. Mỗi test class 1 instance riêng,
 * reset qua reloadFromDisk() trên temp dir mới (EnginePaths.init).
 */
class PluginManagerTest {
    private lateinit var tmp: File
    private val pluginsDir: File get() = File(EnginePaths.dataDir.toFile(), "tools/plugins")
    private val pluginManager = PluginManager(platformEngineLogger())

    @BeforeTest
    fun setUp() {
        tmp = createTempDirectory("leech-plugin-mgr").toFile()
        EnginePaths.init(tmp.path.toPath())
        pluginsDir.mkdirs()
        // Load từ temp dir trống (tránh đọc ~/.leechtext thật + tránh
        // checkUpdate quét repo thật của user)
        pluginManager.initialize()
        resetFromDisk()
    }

    @AfterTest
    fun tearDown() {
        tmp.deleteRecursively()
    }

    /**
     * Reload từ disk và CHỜ xong hẳn (load + dedupe + notify) qua listener — poll list
     * không đủ vì reload là coroutine async, list trống có thể là trạng thái trước load.
     */
    private fun reloadAndWait() {
        val fired = AtomicBoolean(false)
        val listener = PluginManager.PluginListListener { fired.set(true) }
        pluginManager.addListener(listener)
        pluginManager.reloadFromDisk()
        await { fired.get() }
        pluginManager.removeListener(listener)
    }

    /** Xóa state cũ của singleton — reload từ dir hiện tại (trống). */
    private fun resetFromDisk() = reloadAndWait()

    private fun await(cond: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5000
        while (!cond() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue(cond(), "timeout chờ điều kiện async")
    }

    private fun writePluginFile(
        uuid: String,
        name: String,
        version: Double,
        regex: String,
        priority: Int = 0,
        source: String? = null,
    ): File = File(pluginsDir, "$uuid.plugin").apply {
        val src = source?.let { ""","source":"$it"""" } ?: ""
        writeText(
            """{"uuid":"$uuid","name":"$name","version":$version,""" +
                """"regex":"$regex","priority":$priority$src,"chap":"function c(){}"}""",
        )
    }

    @Test
    fun dedupeByNameKeepsHighestVersionAndDeletesLowerFile() {
        // Trùng tên sau normalize: "Truyện CV (vBook)" ≈ "truyện cv"
        writePluginFile("a-loser", "Truyện CV (vBook)", 0.3, "loser.example.com")
        writePluginFile("b-winner", "truyện cv", 1.3, "winner.example.com")

        pluginManager.reloadFromDisk()
        await {
            pluginManager.list().none { it.uuid == "a-loser" } &&
                !File(pluginsDir, "a-loser.plugin").exists()
        }

        val list = pluginManager.list()
        assertEquals(1, list.size)
        assertEquals("truyện cv", list.single().name)
        assertEquals(1.3, list.single().version)
        assertFalse(File(pluginsDir, "a-loser.plugin").exists(), "file bản thấp phải bị xóa")
        assertTrue(File(pluginsDir, "b-winner.plugin").exists(), "file bản cao được giữ")
    }

    @Test
    fun getReturnsPluginMatchingRegexAndHigherPriorityWins() {
        writePluginFile("p-low", "Low", 1.0, TRUYENCV_REGEX, priority = 1)
        writePluginFile("p-high", "High", 1.0, TRUYENCV_REGEX, priority = 5)
        reloadAndWait()
        assertEquals(2, pluginManager.list().size)

        // Regex plugin khớp full URL (matches() anchor 2 đầu)
        val found = pluginManager.get("https://truyencv.com")
        assertNotNull(found)
        assertEquals("High", found.name)
        assertEquals(null, pluginManager.get("https://khac-hoan-toan.com/x"))
    }

    @Test
    fun addWithDuplicateRegexUpdatesExistingInsteadOfDuplicating() {
        pluginManager.add(PluginEntity(uuid = "u1", name = "Old", version = 1.0, regex = TRUYENCV_REGEX, chapGetter = "x"))
        pluginManager.add(PluginEntity(uuid = "u2", name = "New", version = 2.0, regex = TRUYENCV_REGEX, chapGetter = "x"))

        val list = pluginManager.list()
        assertEquals(1, list.size)
        assertEquals("New", list.single().name)
        assertEquals(2.0, list.single().version)
    }

    @Test
    fun addPluginWithoutRegexOrNullIsIgnored() {
        pluginManager.add(PluginEntity(name = "NoRegex"))
        pluginManager.add(null as PluginEntity?)
        assertTrue(pluginManager.list().isEmpty())
    }

    /**
     * Bug "Nâng cấp" no-op (260902): cùng tên + nguồn nhưng regex đổi giữa 2 version
     * → không match regex-update, add thường để lại 2 entry → UI vẫn thấy bản cũ
     * (v1.5) sau khi bấm nâng cấp lên v1.7. add phải dedupe name+host giữ version cao.
     */
    @Test
    fun addUpgradeWithChangedRegexReplacesOldAndKeepsHigherVersion() {
        pluginManager.add(
            PluginEntity(
                uuid = "tf-15",
                name = "truyenfull (vBook)",
                version = 1.5,
                regex = "truyenfull\\.live/truyen-a/[^/]+",
                source = TRUYENFULL_LIVE_SOURCE,
                chapGetter = "x",
            ),
        )
        pluginManager.add(
            PluginEntity(
                uuid = "tf-17",
                name = "truyenfull (vBook)",
                version = 1.7,
                regex = "truyenfull\\.live",
                source = TRUYENFULL_LIVE_SOURCE,
                chapGetter = "x",
            ),
        )

        val list = pluginManager.list()
        assertEquals(1, list.size, "bản cũ cùng name+source phải bị thay")
        assertEquals(1.7, list.single().version)
    }

    @Test
    fun addPathReadsJsonFileAndAddsToList() {
        val f = writePluginFile("from-file", "FromFile", 1.0, "file.example.com")
        pluginManager.add(f.absolutePath)
        assertEquals(listOf("FromFile"), pluginManager.list().map { it.name })
    }

    @Test
    fun removeDeletesFromListAndPluginFile() {
        writePluginFile("r1", "RemoveMe", 1.0, "remove.example.com")
        reloadAndWait()

        val victim = pluginManager.list().single()
        assertTrue(pluginManager.remove(victim))
        assertTrue(pluginManager.list().isEmpty())
        assertFalse(File(pluginsDir, "r1.plugin").exists(), "file .plugin phải bị xóa")
        assertFalse(pluginManager.remove(victim), "remove lần hai trả false")
        assertFalse(pluginManager.remove(null), "remove null trả false")
    }

    @Test
    fun garbagePluginFileIgnoredWhenLoadingFromDisk() {
        File(pluginsDir, "bad.plugin").writeText("{ not json")
        writePluginFile("good", "Good", 1.0, "good.example.com")

        reloadAndWait()

        assertEquals(listOf("Good"), pluginManager.list().map { it.name })
    }

    @Test
    fun listenerReceivesNotificationsAndStopsAfterUnregister() {
        var count = 0
        val listener = PluginManager.PluginListListener { count++ }

        pluginManager.addListener(listener)
        pluginManager.notifyPluginsChanged()
        assertEquals(1, count)

        pluginManager.removeListener(listener)
        pluginManager.notifyPluginsChanged()
        assertEquals(1, count, "sau removeListener không còn notify")
    }

    @Test
    fun addNewPluginNotifiesListener() {
        var count = 0
        val listener = PluginManager.PluginListListener { count++ }
        pluginManager.addListener(listener)
        pluginManager.add(PluginEntity(uuid = "n1", name = "Notify", regex = "notify.example.com", chapGetter = "x"))
        assertEquals(1, count)
        pluginManager.removeListener(listener)
    }

    /**
     * Bug mirror family (2026-08-25): 2 plugin cùng tên KHÁC source (truyenfull.today
     * v0.3 / truyenfull.live v1.3) là 2 plugin thật — dedupe theo name xóa nhầm file
     * bản thấp. Dedupe phải key theo (name + source).
     */
    @Test
    fun dedupeKeepsBothPluginsWithSameNameDifferentSource() {
        writePluginFile("m1", "Truyện Full (vBook)", 0.3, "truyenfull.today/[^/]+", source = "https://truyenfull.today")
        writePluginFile("m2", "Truyện Full (vBook)", 1.3, "truyenfull.live/[^/]+", source = TRUYENFULL_LIVE_SOURCE)

        reloadAndWait()

        assertEquals(2, pluginManager.list().size, "khác source là 2 plugin khác nhau — không dedupe")
        assertTrue(File(pluginsDir, "m1.plugin").exists(), "file bản thấp phải còn")
        assertTrue(File(pluginsDir, "m2.plugin").exists(), "file bản cao phải còn")
    }

    @Test
    fun dedupeStillMergesSameNameSameSourceKeepingHigherVersion() {
        writePluginFile("s1", "X (vBook)", 0.3, "old.example.com/[^/]+", source = "https://same.example.com")
        writePluginFile("s2", "X", 1.3, "new.example.com/[^/]+", source = "https://same.example.com")

        reloadAndWait()

        assertEquals(1, pluginManager.list().size, "cùng source cùng tên → 1")
        assertEquals(1.3, pluginManager.list().single().version, "giữ version cao")
        assertFalse(File(pluginsDir, "s1.plugin").exists(), "file version thấp bị xóa")
    }
}

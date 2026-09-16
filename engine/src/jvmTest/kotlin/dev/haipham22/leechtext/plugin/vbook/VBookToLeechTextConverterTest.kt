package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.plugin.vbook.model.VBookPluginEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val STUB_GETTER_JS = "function execute(url){}"

/**
 * Test VBookToLeechTextConverter — mapping metadata vBook → PluginEntity (viết mới, fixture
 * inline).
 */
class VBookToLeechTextConverterTest {
    private val converter = VBookToLeechTextConverter()

    private fun vbookPlugin() = VBookPluginEntity(
        name = "TruyenFull",
        author = "vBook",
        version = 10,
        source = "https://truyenfull.vision",
        regexp = "truyenfull\\.vision/.*",
        description = "Truyện Full",
        locale = "vi_VN",
        type = "novel",
        scriptContents =
        linkedMapOf(
            "chap" to STUB_GETTER_JS,
            "toc" to STUB_GETTER_JS,
            "detail" to STUB_GETTER_JS,
            "page" to STUB_GETTER_JS,
        ),
    )

    @Test
    fun convertMapsMetadataAndScripts() {
        val entity = converter.convert(vbookPlugin())

        assertEquals("TruyenFull", entity.name)
        assertEquals(1.0, entity.version) // int 10 / 10.0
        assertEquals("https://truyenfull.vision", entity.source)
        assertEquals("truyenfull\\.vision/.*", entity.regex)
        assertEquals("vi", entity.language) // vi_VN → vi
        assertEquals("dich", entity.group) // novel → dich
        assertTrue(entity.supportUpdate)
        assertTrue(entity.checked)
        assertNotNull(entity.uuid)

        // Script contents inline vào getters
        assertEquals(STUB_GETTER_JS, entity.chapGetter)
        assertEquals(STUB_GETTER_JS, entity.tocGetter)
        assertEquals(STUB_GETTER_JS, entity.detailGetter)
        assertEquals(STUB_GETTER_JS, entity.pageGetter)
    }

    @Test
    fun convertGeneratesStableUuid() {
        val plugin = vbookPlugin()
        val uuid1 = converter.convert(plugin).uuid
        val uuid2 = converter.convert(plugin).uuid
        assertEquals(uuid1, uuid2, "UUID nhất quán theo name+version")
    }

    @Test
    fun convertRejectsMissingRequiredFields() {
        // Thiếu name
        val noName = vbookPlugin().copy(name = null)
        assertFailsWith<IllegalArgumentException> { converter.convert(noName) }

        // Thiếu regexp
        val noRegex = vbookPlugin().copy(regexp = null)
        assertFailsWith<IllegalArgumentException> { converter.convert(noRegex) }

        // Thiếu scripts
        val noScripts = vbookPlugin().copy(scriptContents = linkedMapOf())
        assertFailsWith<IllegalArgumentException> { converter.convert(noScripts) }

        // Null plugin
        assertFailsWith<IllegalArgumentException> { converter.convert(null) }
    }

    @Test
    fun convertDefaults() {
        val entity = converter.convert(vbookPlugin().copy(author = null, version = null, locale = null, type = null))
        assertEquals("vBook", entity.author) // fallback author
        assertEquals(1.0, entity.version) // null version → 1.0
        assertEquals("en", entity.language) // null locale → en
        assertEquals("dich", entity.group) // null type → dich
    }
}

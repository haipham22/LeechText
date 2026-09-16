package dev.haipham22.leechtext

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.models.Settings
import dev.haipham22.leechtext.util.LeechJson
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Format-compat gate (eng review 5A): các model Kotlin port phải đọc được
 * ĐÚNG format file do app Java cũ sinh ra. Fixture:
 * - setting.json: template legacy gốc (src/main/resources/dark/leech/res/setting.json)
 * - sample-vbook.plugin: file .plugin THẬT do app đã-fix cài (TruyệnTR)
 * - properties.json: crafted đúng format History.save() của legacy
 */
class FormatCompatTest {
    private fun fixture(name: String): String = javaClass
        .getResourceAsStream("/fixtures/$name")!!
        .bufferedReader(Charsets.UTF_8)
        .readText()

    @Test
    fun settingsParsesLegacySettingJson() {
        val settings = LeechJson.decodeFromString<Settings>(fixture("setting.json"))

        // connection (@SerialName num_conn/re_conn/time_out là string trong legacy —
        // isLenient coerce sang Int field của model port)
        val connection = settings.connection
        assertNotNull(connection)
        assertEquals(5, connection.numConn)
        assertEquals(3, connection.reConn)
        assertEquals(90000, connection.timeOut)

        // other (@SerializedName theme_color, metadata→metaData)
        val other = settings.other
        assertNotNull(other)
        assertEquals("#263238", other.themeColor)
        val trash = other.trash
        assertNotNull(trash)
        assertTrue(trash.isNotEmpty(), "trash list phải parse được từ template")
    }

    @Test
    fun pluginEntityParsesRealPluginFile() {
        val plugin = LeechJson.decodeFromString<PluginEntity>(fixture("sample-vbook.plugin"))

        assertEquals("447b1315-6708-c834-3c71-0dd1c65ab34b", plugin.uuid)
        assertEquals("TruyệnTR", plugin.name)
        assertNotNull(plugin.regex, "plugin thật phải có regex match")
        assertTrue(
            !plugin.chapGetter.isNullOrEmpty() || !plugin.tocGetter.isNullOrEmpty(),
            "plugin thật phải có ít nhất một script getter",
        )
    }

    @Test
    fun propertiesJsonParsesLegacyHistoryFormat() {
        // properties.json do History.save() legacy ghi: org.json, không Gson
        val obj = JSONObject(fixture("properties.json"))

        val metadata = obj.getJSONObject("metadata")
        assertEquals("Thân Đạo Đan Tôn", metadata.getString("name"))
        assertEquals(2247, metadata.getInt("size"))
        assertEquals("https://truyenfull.vision/than-dao-dan-ton", metadata.getString("url"))

        val list = obj.getJSONArray("list")
        assertEquals(2, list.length())
        val ch0 = list.getJSONObject(0)
        assertEquals("C0", ch0.getString("id"))
        assertEquals("Chương 1", ch0.getString("chap"))
        assertEquals(false, ch0.getBoolean("error"))
    }
}

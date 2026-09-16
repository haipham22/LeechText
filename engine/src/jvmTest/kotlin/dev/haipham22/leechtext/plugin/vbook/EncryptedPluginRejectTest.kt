package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.vbook.exception.VBookPluginException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Plugin mã hóa (repo duongden/vbook flag "encrypt": true) phải bị chặn tại install —
 * script là ciphertext, execute làm JS chỉ ra ReferenceError rác (bug 260903).
 * Chạy: ./gradlew :engine:jvmTest --tests "*EncryptedPluginReject*"
 */
class EncryptedPluginRejectTest {
    private val encryptedZip =
        "https://raw.githubusercontent.com/duongden/vbook/main/FixedDomain/truyenfull/plugin.zip"
    private val plainZip =
        "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/truyenfull/plugin.zip"

    @Test
    fun encryptedPluginRejected() {
        // extractFromZip(url) wrap inner message — chỉ cần chặn được, message gốc ở cause
        val e =
            assertFailsWith<VBookPluginException> {
                PluginZipExtractor(platformEngineLogger()).extractFromZip(encryptedZip)
            }
        val full = (listOf(e.message) + generateSequence(e.cause) { it.cause }.map { it.message }).joinToString()
        assertEquals(true, full.contains("Encrypted plugin"), "message phải nói lý do: $full")
    }

    @Test
    fun plainPluginStillPasses() {
        val entity = PluginZipExtractor(platformEngineLogger()).extractFromZip(plainZip)
        assertEquals(true, entity.scriptContents.isNotEmpty())
    }
}

package dev.haipham22.leechtext.action

import dev.haipham22.leechtext.action.export.Ebook
import dev.haipham22.leechtext.action.export.ProgressListener
import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.TypeUtils
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Repro thực tế bug epub 0 byte với dữ liệu Cầu Ma (1470 chương, tên unicode).
 * Bỏ qua nếu máy không có dữ liệu. Chạy:
 * ./gradlew :engine:jvmTest --tests "*EpubRealBook*"
 */
class EpubRealBookTest {
    @Test
    fun epubExportOnRealCauMaData() {
        val dir = File("/home/haips/.leechtext/output/Cau_Ma")
        if (!dir.isDirectory) return // máy khác không có dữ liệu — bỏ qua
        val p = loadHistory(File(dir, "properties.json").path, platformEngineLogger()) ?: return
        assertTrue(p.chapList.orEmpty().isNotEmpty())
        val completedBefore = p.chapList!!.count { it.completed }
        Ebook(
            platformEngineLogger(),
            p,
            TypeUtils.EPUB,
            tool = "Mặc định",
            compressLevel = "6",
            autoSplit = false,
            includeImg = true,
            settings = SettingsRepository.load(),
            progressListener = ProgressListener { _, _ -> },
        ).export()
        val f = File(p.savePath, "out/" + ("${p.name} - ${p.author}.epub".replace("[:/?*]".toRegex(), "")))
        assertTrue(f.exists(), "epub phải tồn tại: ${f.path}")
        assertTrue(f.length() > 100_000, "epub phải lớn (raw ~20MB), được ${f.length()} — completed trước export: $completedBefore")
    }
}

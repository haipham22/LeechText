package dev.haipham22.leechtext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * QA UI thay wasm preview (P4 decision: wasm CUT — screens ở jvmMain gọi engine trực
 * tiếp, không migrate chỉ để preview). Render offscreen SliceApp từng tab → PNG,
 * assert KHÔNG trắng trơn (screen compose + render thật, không cần WindowServer).
 * Chạy: ./gradlew :app-shared:jvmTest --tests "*Screenshot*"
 */
class SliceAppScreenshotTest {
    @Test
    fun renderDesktopLibrary() = runBlocking {
        File("/tmp/lt-app.png").writeBytes(renderBytes(width = 1280, height = 800, tab = 0, settleFrames = 30))
    }

    @Test
    fun renderMobileLibrary() = runBlocking {
        File("/tmp/lt-mobile.png").writeBytes(renderBytes(width = 390, height = 844, tab = 0, settleFrames = 30))
    }

    /** Per-screen smoke: 0 Thư viện, 1 Cập nhật, 2 Lịch sử, 3 Nguồn, 4 Cài đặt. */
    @Test
    fun renderEveryTabNonBlank() = runBlocking {
        val names = listOf("library", "queue", "history", "sources", "settings")
        for (tab in names.indices) {
            val png = renderBytes(width = 1280, height = 800, tab = tab, settleFrames = 12)
            File("/tmp/lt-tab-${names[tab]}.png").writeBytes(png)
            val colors = distinctColors(png)
            // ponytail: ngưỡng 8 — blank thật (chỉ nền) = 2-5 màu; empty state thưa
            // (text + rail + icon) ~15-25 màu. Nâng khi cần visual-regression thật.
            assertTrue(colors >= 8, "tab ${names[tab]} khả nghi trắng trơn (chỉ $colors màu)")
        }
    }

    private suspend fun renderBytes(
        width: Int,
        height: Int,
        tab: Int,
        settleFrames: Int,
    ): ByteArray {
        val scene =
            ImageComposeScene(width = width, height = height) {
                // Scene offscreen trong suốt — vẽ nền canvas như window thật
                Box(Modifier.fillMaxSize().background(Color(0xFFF7FAF8))) {
                    SliceApp(
                        RootComponent(
                            dev.haipham22.leechtext.testComponentContext(),
                            initialTab = tab,
                            log = dev.haipham22.leechtext.log.platformEngineLogger(),
                            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                            pluginUpdate =
                            dev.haipham22.leechtext.plugin.PluginUpdate(
                                dev.haipham22.leechtext.log.platformEngineLogger(),
                                dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                                dev.haipham22.leechtext.plugin.RepositoryManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                            ),
                            repositoryManager = dev.haipham22.leechtext.plugin.RepositoryManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                        ),
                    )
                }
            }
        delay(if (settleFrames > 12) 4000 else 2500) // đợi font load + state init + cover fetch
        // Warm-up: scene cần vài frame advance để composition + animation settle
        var png: org.jetbrains.skia.Data? = null
        for (frame in 0..settleFrames) {
            png = scene.render(frame * 16_000_000L).encodeToData(EncodedImageFormat.PNG)
            delay(100)
        }
        scene.close()
        return png?.bytes ?: ByteArray(0)
    }

    /** Đếm số màu distinct (sample grid) — blank/solid canvas = vài màu, UI thật = nhiều. */
    private fun distinctColors(png: ByteArray): Int {
        val image = Image.makeFromEncoded(png)
        val bmp = Bitmap()
        bmp.allocN32Pixels(image.width, image.height, false)
        check(image.readPixels(bmp)) { "readPixels fail" }
        val info = ImageInfo.makeN32(image.width, image.height, ColorAlphaType.OPAQUE)
        val bytes = bmp.readPixels(info, image.width * 4, 0, 0) ?: error("readPixels bytes fail")
        val colors = HashSet<Int>()
        var i = 0
        val stride = 4 * 64 // sample mỗi 64 pixel
        while (i + 3 < bytes.size) {
            colors.add(((bytes[i].toInt() and 0xFF) shl 16) or ((bytes[i + 1].toInt() and 0xFF) shl 8) or (bytes[i + 2].toInt() and 0xFF))
            i += stride
        }
        return colors.size
    }
}

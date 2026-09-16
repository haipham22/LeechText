package dev.haipham22.leechtext.ui

import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/**
 * Actual iOS cho các expect app-shared (P5.3).
 * Picker: chưa có UI native — trả null, caller fallback user gõ tay path
 * (cùng behavior Android hiện tại).
 */

/** iOS: decode qua Skia (skiko bundle với Compose iOS) — giống desktop. */
actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? = runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

/** iOS: không foreground service — trả false để queue tự tải bằng coroutine (như desktop). */
@Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant") // parity expect/actual — Android service sau này dùng
actual fun startPlatformDownload(
    url: String,
    chapterRange: String?,
): Boolean = false // NOSONAR — expect/actual symmetry

/** Chỉ desktop — ẩn cài đặt chỉ desktop (Calibre/Kindlegen). */
actual val IS_DESKTOP_PLATFORM: Boolean = false

/** iOS marker true. */
actual val IS_IOS: Boolean = true

/** iOS: UIApplication.openURL. */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun openUrl(url: String) {
    runCatching {
        val nsUrl = platform.Foundation.NSURL(string = url)
        platform.UIKit.UIApplication.sharedApplication.openURL(nsUrl)
    }
}

/** iOS: share sheet (UIActivityViewController) từ root VC. */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun shareFile(path: String) {
    runCatching {
        val url = platform.Foundation.NSURL.fileURLWithPath(path)
        val activity = platform.UIKit.UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(activity, animated = true, completion = null)
    }
}

/** iOS: UIDocumentPicker save — chọn chỗ lưu file export (dogfood 260903:
 * fallback null khiến iOS chỉ gõ tay path). asCopy=0 → URL là chỗ user chọn,
 * app tự ghi file sau đó; security-scoped resource không cần vì ghi 1 lần.
 * ponytail: bookmark/persist access chưa cần — export ghi ngay sau khi pick. */
// title: desktop FileDialog dùng — iOS UIDocumentPicker không có title (parity expect/actual)
@Suppress("UNUSED_PARAMETER")
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual fun pickSaveFile(
    title: String,
    defaultName: String,
    onResult: (String?) -> Unit,
) {
    runCatching {
        // Xcode 26 SDK: constructor init(forContentTypes:asCopy:) — Swift tiện ích
        // forExporting không xuất hiện trong Kotlin cinterop; contentTypes rỗng =
        // để hệ thống suy từ đuôi file của defaultName
        val picker = platform.UIKit.UIDocumentPickerViewController(
            forOpeningContentTypes = emptyList<kotlinx.cinterop.ObjCObject>(),
            asCopy = true,
        )
        picker.delegate =
            object : platform.darwin.NSObject(), platform.UIKit.UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: platform.UIKit.UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>,
                ) {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? platform.Foundation.NSURL
                    onResult(url?.path)
                }

                override fun documentPickerWasCancelled(controller: platform.UIKit.UIDocumentPickerViewController) {
                    onResult(null)
                }
            }
        platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }.onFailure { onResult(null) }
}

// title: desktop JFileChooser dùng — iOS chưa hỗ trợ chọn thư mục (parity expect/actual)
@Suppress("UNUSED_PARAMETER")
internal actual fun pickDirectory(
    title: String,
    onResult: (String?) -> Unit,
) {
    onResult(null)
}

// title: desktop FileDialog dùng — iOS photo picker riêng không nhận title (parity expect/actual)
@Suppress("UNUSED_PARAMETER")
internal actual fun pickImageFile(
    title: String,
    onResult: (String?) -> Unit,
) {
    onResult(null)
}

/** iOS: chưa override locale — dùng locale hệ thống. language: JVM/Android shim dùng. */
@Suppress("UNUSED_PARAMETER") // parity expect/actual
internal actual fun appLanguageProviders(language: String): Array<ProvidedValue<*>> = emptyArray()

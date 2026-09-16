package dev.haipham22.leechtext.ui.reader

import androidx.compose.ui.text.font.FontFamily
import okio.FileSystem
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject

/**
 * iOS font import (owner 2026-08-29): "Thêm font…" mở Files picker (.ttf/.otf) →
 * copy vào fonts dir app → font vào cycle reader ngay.
 */

/** Root VC — keyWindow deprecated iOS 13 nhưng vẫn hoạt động, K/N NSSet iteration phiền. */
private fun rootViewController(): UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController

/**
 * "Mở thư mục font" trên iOS = picker import. asCopy=true → hệ thống tự copy ra
 * temp (không cần security scope), mình copy tiếp vào fonts dir của app.
 */
actual fun openFontsFolder() {
    val types =
        listOfNotNull(
            UTType.typeWithIdentifier("public.truetype-font"),
            UTType.typeWithIdentifier("public.opentype-font"),
        ).distinct()
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
    picker.allowsMultipleSelection = false
    picker.delegate =
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                val name = url.lastPathComponent ?: return
                if (!name.endsWith(".ttf", true) && !name.endsWith(".otf", true)) return
                runCatching {
                    FileSystem.SYSTEM.createDirectories(UserFonts.dir)
                    val data = NSData.dataWithContentsOfFile(url.path ?: return) ?: return
                    data.writeToFile((UserFonts.dir / name).toString(), atomically = true)
                }
            }
        }
    rootViewController()?.presentViewController(picker, animated = true, completion = null)
}

/** Font user từ file trong fonts dir — okio đọc bytes, skia decode. */
actual fun userFontFamily(fileName: String): FontFamily? = runCatching {
    val path = UserFonts.dir / fileName
    val fs = FileSystem.SYSTEM
    if (!fs.exists(path)) return null
    val bytes = fs.read(path) { readByteArray() }
    FontFamily(androidx.compose.ui.text.platform.Font(identity = fileName, data = bytes))
}.getOrNull()

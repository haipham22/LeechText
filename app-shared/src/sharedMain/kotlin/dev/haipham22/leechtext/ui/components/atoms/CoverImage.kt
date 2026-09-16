package dev.haipham22.leechtext.ui.components.atoms
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.Http
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.cd_cover
import dev.haipham22.leechtext.ui.IoDispatcher
import dev.haipham22.leechtext.ui.decodeImageBitmap
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.stringResource

/**
 * Cache cover toàn app — chống refetch khi composable dispose/recreate
 * (branch switch BoxWithConstraints, grid recycle, đổi tab).
 * ponytail: không LRU eviction — vài trăm cover ~vài chục MB, thêm khi thiếu memory.
 */
// ponytail: HashMap trần — chỉ truy cập từ main thread (LaunchedEffect), không cần concurrent map
private val coverCache = HashMap<String, ImageBitmap>()

/** Prefix cover file local (user chọn ảnh thay thế) — strip trước khi đọc disk. */
private const val FILE_URL_PREFIX = "file://"

/** Xóa cache 1 cover — đổi ảnh cùng path (replaceCover) phải đọc lại disk,
 * không thì bitmap cũ hiển thị mãi (bug 260906: phải thoát vào lại truyện). */
fun invalidateCoverCache(url: String?) {
    if (url != null) coverCache.remove(url)
}

/**
 * Hiển thị cover từ URL — cache static, decode qua expect/actual
 * (Skia desktop / BitmapFactory Android). Null/failed → placeholder skeleton
 * (surfaceContainerHigh) đúng kích thước ô, không nhảy layout.
 */

/** Fetch + decode cover về bitmap — data URI decode trực tiếp, file path (cover
 * thay thế do user chọn) đọc disk, còn lại qua HTTP. Null = trống/lỗi. */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
private fun decodeDataUri(url: String): ByteArray? = runCatching {
    kotlin.io.encoding.Base64.decode(url.substringAfter(","))
}.getOrNull()

private suspend fun fetchCoverBitmap(
    url: String,
    log: EngineLogger,
): ImageBitmap? = withContext(IoDispatcher) {
    runCatching {
        val bytes =
            when {
                url.startsWith("data:") -> decodeDataUri(url)

                url.startsWith("/") || url.startsWith(FILE_URL_PREFIX) ->
                    FileSystem.SYSTEM.read(url.removePrefix(FILE_URL_PREFIX).toPath()) { readByteArray() }

                else -> Http(log).get(url).bytes()
            }
        if (bytes != null && bytes.size > 0) decodeImageBitmap(bytes) else null
    }.getOrNull()
}

@Composable
fun CoverImage(
    url: String?,
    log: EngineLogger,
    modifier: Modifier = Modifier,
    size: Dp? = null,
) {
    // File local (cover user thay) → bust theo mtime: replaceCover ghi đè CÙNG path,
    // remember(url) + cache theo url giữ bitmap cũ mãi (bug 260906: thoát vào lại mới thấy)
    val fileBust =
        url?.takeIf { it.startsWith("/") || it.startsWith(FILE_URL_PREFIX) }?.let {
            runCatching {
                FileSystem.SYSTEM.metadataOrNull(it.removePrefix(FILE_URL_PREFIX).toPath())?.lastModifiedAtMillis
            }.getOrNull()
        }
    var bitmap by remember(url, fileBust) { mutableStateOf(coverCache[url]) }
    var failed by remember(url, fileBust) { mutableStateOf(false) }
    LaunchedEffect(url, fileBust) {
        if (url.isNullOrBlank() || bitmap != null) return@LaunchedEffect
        val bmp = fetchCoverBitmap(url, log)
        if (bmp != null) {
            coverCache[url] = bmp
            bitmap = bmp
        } else {
            failed = true
        }
    }
    val sized = if (size != null) modifier.size(size) else modifier
    val img = bitmap
    if (img != null) {
        Image(
            bitmap = img,
            contentDescription = stringResource(Res.string.cd_cover),
            modifier = sized,
            contentScale = ContentScale.Crop,
        )
    } else {
        // Placeholder + spinner nhỏ khi đang fetch (đỡ nhìn "ô trống chết");
        // fetch fail → skeleton tĩnh, không xoay vĩnh viễn
        Box(sized.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = androidx.compose.ui.Alignment.Center) {
            if (!url.isNullOrBlank() && !failed) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

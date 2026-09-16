package dev.haipham22.leechtext.ui.reader
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.chapter_default_name
import dev.haipham22.leechtext.resources.lib_chapter_not_downloaded
import dev.haipham22.leechtext.ui.library.LibraryState
import org.jetbrains.compose.resources.stringResource

/**
 * Chia dòng của 1 đoạn văn vào các trang (chế độ paged): greedy đổ dòng vào trang
 * đến khi vượt pageHeight, dòng không cắt đôi. offset = phần cao đã dùng ở trang hiện tại
 * (ví dụ title ở đầu trang 1). Luôn trả ≥ 1 range (đoạn rỗng → 1 trang 0 dòng).
 */
internal fun splitLinesByPage(
    lineCount: Int,
    lineHeight: Float,
    pageHeight: Float,
    offset: Float,
): List<IntRange> {
    if (lineCount <= 0) return listOf(0..-1)
    if (pageHeight <= 0f) return listOf(0 until lineCount)
    val pages = mutableListOf<IntRange>()
    var start = 0
    var used = offset.coerceIn(0f, pageHeight)
    var i = 0
    while (i < lineCount) {
        if (used + lineHeight > pageHeight && i > start) {
            pages += start until i // đóng trang [start, i-1], dòng i sang trang kế
            start = i
            used = 0f
        }
        // Đặt dòng i — trang trống mà vẫn tràn (offset cao/line cao) thì chấp nhận overflow,
        // không tạo trang 0 dòng
        used += lineHeight
        i++
    }
    pages += start until lineCount
    return pages
}

/** 1 slice nội dung trên trang: title == true → tên chương (chỉ trang đầu), else text đoạn. */
internal data class ReaderPageSlice(
    val title: Boolean,
    val text: String,
)

/**
 * Ghép title + các đoạn thành trang. Mỗi layout (đoạn) chia dòng theo splitLinesByPage,
 * dòng của 1 đoạn nằm liền nhau; cách đoạn = spacing param (px). lineHeight của layout
 * = getLineBottom(0) (dòng đều nhau cùng style). Trả pages rỗng nếu pageHeight ≤ 0.
 */
private fun buildPages(
    title: androidx.compose.ui.text.TextLayoutResult,
    paragraphs: List<androidx.compose.ui.text.TextLayoutResult>,
    pageHeight: Float,
    paragraphSpacingPx: Float,
    titleSpacingPx: Float,
): List<List<ReaderPageSlice>> {
    if (pageHeight <= 0f) return emptyList()
    val pages = mutableListOf<List<ReaderPageSlice>>()
    var current = mutableListOf<ReaderPageSlice>()
    var used = 0f

    fun newPage() {
        if (current.isNotEmpty()) pages += current
        current = mutableListOf()
        used = 0f
    }

    // Title — cắt theo dòng như đoạn thường, flag title để render style titleLarge
    val titleLh = title.getLineBottom(0)
    splitLinesByPage(title.lineCount, titleLh, pageHeight, 0f).forEachIndexed { pi, range ->
        if (pi > 0) newPage()
        val slice = sliceOf(title, range)
        if (slice.isNotEmpty()) current += ReaderPageSlice(title = true, text = slice)
        used += range.count() * titleLh + titleSpacingPx
    }

    paragraphs.forEach { layout ->
        // mỗi range của 1 đoạn = 1 trang mới (range trước đã đầy)
        layoutSlices(layout, pageHeight, used).forEach { slice ->
            if (current.isNotEmpty() && used + slice.height > pageHeight) newPage()
            if (slice.text.isNotEmpty()) current += ReaderPageSlice(title = false, text = slice.text)
            used += slice.height + paragraphSpacingPx
        }
    }
    if (current.isNotEmpty()) pages += current
    return pages
}

/** 1 slice đoạn: text theo range dòng + chiều cao range (px). */
private data class ReaderSlice(val text: String, val height: Float)

/** Chia 1 layout thành các slice theo trang bắt đầu tại offset — mỗi slice 1 trang mới. */
private fun layoutSlices(
    layout: androidx.compose.ui.text.TextLayoutResult,
    pageHeight: Float,
    offset: Float,
): List<ReaderSlice> {
    val lh = if (layout.lineCount > 0) layout.getLineBottom(0) else 0f
    return splitLinesByPage(layout.lineCount, lh, pageHeight, offset).map { range ->
        ReaderSlice(text = sliceOf(layout, range), height = range.count() * lh)
    }
}

/** Text của range dòng trong layout — cắt theo biên line, trim 2 đầu. */
private fun sliceOf(
    layout: androidx.compose.ui.text.TextLayoutResult,
    range: IntRange,
): String = layout.layoutInput.text
    .substring(layout.getLineStart(range.first).toInt(), layout.getLineEnd(range.last, visibleEnd = true).toInt())
    .trim()

/**
 * Body chế độ paged (Kindle-style): đo text chia trang theo viewport, HorizontalPager
 * lật ngang. Pages tính bằng TextMeasurer — nhớ theo (text, display, size) để đổi
 * font/size/cửa sổ tính lại. page/onPage/onTotal sync 2 chiều với mức reader
 * (phím lật + progress ở ngoài, swipe ở trong).
 */
@Composable
internal fun ReaderPagedBody(
    state: LibraryState,
    content: ReaderChapterContent,
    display: ReaderDisplay,
    pagedState: PagedState,
    showChrome: Boolean,
    modifier: Modifier = Modifier,
) {
    val fgColor = readerFgColor(display.fgKey, ReaderBg.fromKey(display.bgKey).scheme().background)
    val chapter = content.chapter
    val paragraphs = content.paragraphs
    val fontFamily = readerFontFamily(display.fontKey)
    val titleStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily)
    val bodyStyle =
        MaterialTheme.typography.bodyLarge.copy(
            fontSize = display.fontSize.sp,
            lineHeight = (display.fontSize * display.lineHeight).sp,
            fontFamily = fontFamily,
        )
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val pageW = constraints.maxWidth - 2 * with(density) { 24.dp.toPx() }
        val pageH = constraints.maxHeight - with(density) { (if (showChrome) 20.dp else 52.dp).toPx() + 20.dp.toPx() }
        // Đo + chia trang: title đo riêng (spacing 16dp), đoạn spacing 12dp như LazyColumn
        val pages: List<List<ReaderPageSlice>> =
            remember(content, display.fontSize, display.fontKey, display.lineHeight, pageW, pageH, showChrome) {
                if (pageW <= 0 || pageH <= 0) return@remember emptyList()
                val maxWidth = Constraints(maxWidth = pageW.toInt())
                val titleLayout = textMeasurer.measure(chapter.chapName ?: "", titleStyle, constraints = maxWidth)
                val paragraphLayouts = paragraphs.map { textMeasurer.measure(it.trim(), bodyStyle, constraints = maxWidth) }
                buildPages(
                    titleLayout,
                    paragraphLayouts,
                    pageH,
                    paragraphSpacingPx = with(density) { 12.dp.toPx() },
                    titleSpacingPx = with(density) { 16.dp.toPx() },
                )
            }
        ReaderPager(
            pages,
            pagedState.page,
            onPage = { pagedState.page = it },
            onTotal = { pagedState.total = it },
            theme =
            ReaderPageTheme(titleStyle, bodyStyle, fgColor, content.idx, showChrome, paragraphs.isEmpty(), state.busy),
        )
    }
}

/** Style + nội dung render 1 trang pager (gom param cho ReaderPager, S107). */
internal class ReaderPageTheme(
    val titleStyle: androidx.compose.ui.text.TextStyle,
    val bodyStyle: androidx.compose.ui.text.TextStyle,
    val fgColor: Color?,
    val chapterIdx: Int,
    val showChrome: Boolean,
    val notDownloaded: Boolean,
    val busy: Boolean,
)

/** HorizontalPager đồng bộ 2 chiều page với mức reader (phím ngoài, swipe trong). */
@Composable
internal fun ReaderPager(
    pages: List<List<ReaderPageSlice>>,
    page: Int,
    onPage: (Int) -> Unit,
    onTotal: (Int) -> Unit,
    theme: ReaderPageTheme,
) {
    val onTotalLatest = rememberUpdatedState(onTotal)
    val onPageLatest = rememberUpdatedState(onPage)
    LaunchedEffect(pages.size) { onTotalLatest.value(pages.size.coerceAtLeast(1)) }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(page.coerceIn(0, (pages.size - 1).coerceAtLeast(0))) { pages.size.coerceAtLeast(1) }
    // Phím lật ngoài → animate tới trang; swipe trong → báo page mới ra ngoài
    LaunchedEffect(page) {
        if (page != pagerState.currentPage && page in 0 until pagerState.pageCount) {
            pagerState.animateScrollToPage(page)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != page) onPageLatest.value(pagerState.currentPage)
    }
    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = if (theme.showChrome) 20.dp else 52.dp, bottom = 20.dp),
        ) {
            PageContent(pages.getOrNull(p), theme)
            // Chưa tải — hint như chế độ dọc
            if (theme.notDownloaded) {
                if (theme.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    stringResource(Res.string.lib_chapter_not_downloaded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Slices của 1 trang → Text title/đoạn (tách cho S3776). */
@Composable
private fun PageContent(
    slices: List<ReaderPageSlice>?,
    theme: ReaderPageTheme,
) {
    slices?.forEach { slice ->
        if (slice.title) {
            Text(
                slice.text.ifEmpty { stringResource(Res.string.chapter_default_name, theme.chapterIdx + 1) },
                style = theme.titleStyle,
                color = theme.fgColor ?: Color.Unspecified,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        } else {
            Text(slice.text, style = theme.bodyStyle, color = theme.fgColor ?: Color.Unspecified)
        }
    }
}

/** Body đọc (không edit): dọc LazyColumn / paged HorizontalPager + tap-toggle chrome fullscreen. */

/** Trạng thái trang paged mức reader — page/total + callback (gom param cho ReaderBody). */
internal class PagedState {
    var page by mutableIntStateOf(0)
    var total by mutableIntStateOf(1)

    /** Lật trang: ngoài [0, total) → đổi chương qua [onChapter]; trong → đặt page. */
    fun turn(
        delta: Int,
        chapterIdx: Int,
        chapterTotal: Int,
        onChapter: (Int) -> Unit,
    ) {
        val next = page + delta
        when {
            next < 0 && chapterIdx > 0 -> onChapter(-1)
            next >= total && chapterIdx < chapterTotal - 1 -> onChapter(1)
            next >= 0 && next < total -> page = next
        }
    }
}

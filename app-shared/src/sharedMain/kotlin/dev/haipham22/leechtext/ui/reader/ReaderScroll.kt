package dev.haipham22.leechtext.ui.reader
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import dev.haipham22.leechtext.ui.library.LibraryState
import dev.haipham22.leechtext.util.SettingsRepository
import kotlinx.coroutines.isActive

/**
 * Bước cuộn theo phím: ↑/↓ = ⅛ viewport (đọc tiện theo dòng), Page/Space = 90%
 * (chừa 10% dòng cuối sang trang sau, chuẩn reader). Viewport ≤ 0 → 0 = bỏ qua.
 */
internal fun readerScrollStep(
    viewport: Int,
    page: Boolean,
): Int = if (viewport <= 0) {
    0
} else if (page) {
    viewport * 9 / 10
} else {
    viewport / 8
}

/** px cuộn mỗi frame từ tốc độ 1..10 — nhân theo dt frame thực để độc lập tần số quét (120Hz không nhanh gấp đôi). */
internal fun autoScrollStep(
    speed: Int,
    frameDtNanos: Long,
): Float = speed.coerceIn(1, 10) * frameDtNanos / 16_666_667f

/** Hết list (auto-scroll chạm đáy) còn chương kế → true. */
internal fun shouldAutoAdvance(
    canScrollForward: Boolean,
    idx: Int,
    total: Int,
): Boolean = !canScrollForward && idx < total - 1

/**
 * Auto-scroll state (chế độ dọc): enabled không persist (mở app tự cuộn là hostile),
 * speed persist qua SettingsRepository. Tách khỏi ChapterReader() cho complexity (S3776).
 */
internal class AutoScroller(initialSpeed: Int) {
    var enabled by mutableStateOf(false)

    var speed: Int = initialSpeed
        private set

    fun setSpeed(n: Int) {
        speed = n.coerceIn(1, 10)
        dev.haipham22.leechtext.util.SettingsRepository.save(
            dev.haipham22.leechtext.util.SettingsRepository.load().copy(readerAutoScrollSpeed = speed),
        )
    }

    fun toggle() {
        enabled = !enabled
    }

    companion object {
        fun load(): AutoScroller = AutoScroller(
            dev.haipham22.leechtext.util.SettingsRepository.load().readerAutoScrollSpeed.coerceIn(1, 10),
        )
    }
}

/**
 * Vòng lặp auto-scroll: cuộn theo dt frame (độc lập tần số quét), hết chương grace 1.5s
 * rồi auto-next. listState re-key theo idx là load-bearing: changeChapter tạo listState
 * mới → effect restart → cuộn tiếp từ đầu chương mới.
 */
@Composable
internal fun AutoScrollLoop(
    auto: AutoScroller,
    state: LibraryState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    idx: Int,
    total: Int,
    paused: Boolean,
) {
    LaunchedEffect(auto.enabled, paused, listState) {
        if (!auto.enabled || paused) return@LaunchedEffect
        var last = -1L
        while (isActive) {
            val now = withFrameNanos { it } // mất focus cửa sổ → frame clock stall → tự pause
            if (last > 0) {
                if (listState.canScrollForward) {
                    // speed đọc mỗi frame — ↑/↓ đổi tốc độ ăn ngay, không restart effect
                    listState.scrollBy(autoScrollStep(auto.speed, now - last))
                } else if (handleListEnd(auto, state, listState, idx, total)) {
                    return@LaunchedEffect
                }
            }
            last = now
        }
    }
}

/**
 * Chạm đáy list: grace 1.5s cho user thấy hết chương, vẫn đáy → auto sang chương kế
 * (còn) hoặc tự tắt (hết sách). Trả true = kết thúc effect.
 */
private suspend fun handleListEnd(
    auto: AutoScroller,
    state: LibraryState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    idx: Int,
    total: Int,
): Boolean {
    kotlinx.coroutines.delay(1500) // grace cho user thấy hết chương
    if (listState.canScrollForward) return false // user cuộn ngược lên trong grace → tiếp tục
    if (shouldAutoAdvance(false, idx, total)) {
        state.changeChapter(1)
    } else {
        auto.enabled = false // hết sách — tự tắt
    }
    return true
}

/** Phím → hành động phân loại — scroll tách bước để chapterNavKeys resolve viewport lazy. */
internal sealed interface ReaderKeyAction {
    data object Exit : ReaderKeyAction

    data object ToggleAutoScroll : ReaderKeyAction

    data class SpeedDelta(val delta: Int) : ReaderKeyAction

    data class ChapterDelta(val delta: Int) : ReaderKeyAction

    data class PageTurn(val delta: Int) : ReaderKeyAction

    /** Cuộn — page=true = 90% viewport (Space/Page), false = ⅛ (↑/↓); âm = ngược. */
    data class Scroll(val page: Boolean, val forward: Boolean) : ReaderKeyAction
}

/** Hành động phím reader — gom callback cho chapterNavKeys (S107/S3776). */
internal class ReaderKeyActions(
    val onDelta: (Int) -> Unit,
    val onExit: () -> Unit,
    val onScroll: (Int) -> Unit,
    val onToggleAutoScroll: () -> Unit,
    val onSpeedDelta: (Int) -> Unit,
    val onPageTurn: (Int) -> Unit,
)

/**
 * Map phím → action. ↑/↓ đổi tốc độ khi auto đang bật (đặt trước branch cuộn — mới thắng cũ);
 * paged: ←/→ + Page/Space lật trang; vertical: ←/→ chương, ↑/↓ + Page/Space cuộn.
 */

/** Hướng logic phím: forward = →/Space/PageDown, back = ←/Shift+Space/PageUp; ↑/↓ riêng. */
private fun keyDirection(e: androidx.compose.ui.input.key.KeyEvent): Int? = when (e.key) {
    Key.DirectionDown -> -1
    Key.DirectionUp -> 1
    Key.DirectionRight, Key.PageDown -> 1
    Key.DirectionLeft, Key.PageUp -> -1
    Key.Spacebar -> if (e.isShiftPressed) -1 else 1
    else -> null
}

/** Key event → action — tách lookup paged/vertical khỏi khối exit/auto cho S3776. */
private fun readerKeyAction(
    e: androidx.compose.ui.input.key.KeyEvent,
    idx: Int,
    total: Int,
    exitEnabled: Boolean,
    scrollEnabled: Boolean,
    autoScroll: Boolean,
    paged: Boolean,
): ReaderKeyAction? {
    if (e.type != KeyEventType.KeyDown) return null
    if (e.key == Key.Escape && exitEnabled) return ReaderKeyAction.Exit
    // A toggle auto-scroll — gate scrollEnabled kẻo edit mode nuốt chữ "a" (tunneling)
    if (scrollEnabled && !paged && e.key == Key.A) return ReaderKeyAction.ToggleAutoScroll
    val dir = keyDirection(e) ?: return null
    return readerDirectionAction(e, dir, idx, total, scrollEnabled, autoScroll, paged)
}

/** Phím có hướng (↑↓←→/Space/Page) → action theo mode + trạng thái. */
private fun readerDirectionAction(
    e: androidx.compose.ui.input.key.KeyEvent,
    dir: Int,
    idx: Int,
    total: Int,
    scrollEnabled: Boolean,
    autoScroll: Boolean,
    paged: Boolean,
): ReaderKeyAction? {
    val vertical = e.key == Key.DirectionUp || e.key == Key.DirectionDown
    return when {
        // ↑/↓ đổi tốc độ khi auto đang bật (đặt trước branch cuộn — mới thắng cũ)
        autoScroll && scrollEnabled && !paged && vertical -> ReaderKeyAction.SpeedDelta(dir)

        // Paged: ←/→ + Page/Space lật trang (hết trang sang chương kề trong turnPage)
        paged && scrollEnabled -> ReaderKeyAction.PageTurn(dir)

        // Vertical: ←/→ chương (giới hạn đầu/cuối)
        !vertical && dir > 0 && idx < total - 1 -> ReaderKeyAction.ChapterDelta(1)

        !vertical && dir < 0 && idx > 0 -> ReaderKeyAction.ChapterDelta(-1)

        // ↑/↓ cuộn ⅛ viewport; Space/Page cuộn 90% (Shift = ngược, chuẩn browser)
        scrollEnabled -> ReaderKeyAction.Scroll(page = !vertical, forward = dir > 0)

        else -> null
    }
}

/** Ngữ cảnh phím reader: vị trí chương + mode + gate (gom param cho chapterNavKeys, S107). */
internal class ReaderKeyContext(
    val idx: Int,
    val total: Int,
    val exitEnabled: Boolean,
    val scrollEnabled: Boolean,
    val autoScroll: Boolean,
    val paged: Boolean,
)

internal fun Modifier.chapterNavKeys(
    ctx: ReaderKeyContext,
    viewport: () -> Int,
    actions: ReaderKeyActions,
): Modifier = onPreviewKeyEvent { e ->
    when (val act = readerKeyAction(e, ctx.idx, ctx.total, ctx.exitEnabled, ctx.scrollEnabled, ctx.autoScroll, ctx.paged)) {
        null -> false

        ReaderKeyAction.Exit -> {
            actions.onExit()
            true
        }

        ReaderKeyAction.ToggleAutoScroll -> {
            actions.onToggleAutoScroll()
            true
        }

        is ReaderKeyAction.SpeedDelta -> {
            actions.onSpeedDelta(act.delta)
            true
        }

        is ReaderKeyAction.ChapterDelta -> {
            actions.onDelta(act.delta)
            true
        }

        is ReaderKeyAction.PageTurn -> {
            actions.onPageTurn(act.delta)
            true
        }

        is ReaderKeyAction.Scroll -> {
            val step = readerScrollStep(viewport(), act.page) * if (act.forward) 1 else -1
            actions.onScroll(step)
            true
        }
    }
}

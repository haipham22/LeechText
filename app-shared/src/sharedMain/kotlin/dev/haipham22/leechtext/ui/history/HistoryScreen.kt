package dev.haipham22.leechtext.ui.history
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.chapter_default_name
import dev.haipham22.leechtext.resources.history_empty
import dev.haipham22.leechtext.resources.history_empty_hint
import dev.haipham22.leechtext.resources.history_title
import dev.haipham22.leechtext.resources.time_days_ago
import dev.haipham22.leechtext.resources.time_hours_ago
import dev.haipham22.leechtext.resources.time_just_now
import dev.haipham22.leechtext.resources.time_minutes_ago
import dev.haipham22.leechtext.ui.components.atoms.CoverImage
import dev.haipham22.leechtext.ui.library.LibraryState
import dev.haipham22.leechtext.ui.theme.MonoFont
import dev.haipham22.leechtext.util.readTextOrNull
import okio.FileSystem
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.stringResource

private val fs: okio.FileSystem get() = okio.FileSystem.SYSTEM

private fun fileIn(dir: String?, child: String): okio.Path = (dir ?: "").toPath() / child

/** Màn lịch sử — những truyện đã đọc (có lastread.txt), mới đọc nhất trên cùng. */
@Composable
fun HistoryScreen(
    state: LibraryState,
    onOpenBook: (Properties) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { state.refresh() }

    // lastread.txt tồn tại = đã từng mở đọc; mtime file = lần đọc cuối
    val read =
        state.books
            .mapNotNull { b ->
                val f = fileIn(b.savePath, "lastread.txt")
                if (fs.exists(f)) b to (fs.metadataOrNull(f)?.lastModifiedAtMillis ?: 0L) else null
            }.sortedByDescending { it.second }

    Column(modifier.fillMaxSize()) {
        Text(
            stringResource(Res.string.history_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
        if (read.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(bottom = 120.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.history_empty),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.history_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
            ) {
                items(read, key = { (b, _) -> b.url ?: b.name ?: b.hashCode().toString() }) { (b, time) ->
                    val idx =
                        runCatching {
                            ((b.savePath ?: "").toPath() / "lastread.txt").readTextOrNull()?.trim()?.toIntOrNull()
                        }.getOrNull()
                    HistoryRow(b, idx, time, log = state.log) {
                        onOpenBook(b) // root wire: openBook + resumeReading + tab Library
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    book: Properties,
    chapterIdx: Int?,
    time: Long,
    log: dev.haipham22.leechtext.log.EngineLogger,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            book.cover,
            log = log,
            modifier = Modifier.size(width = 48.dp, height = 72.dp).clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                book.name ?: "?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                book.chapList?.getOrNull(chapterIdx ?: -1)?.chapName
                    ?: stringResource(Res.string.chapter_default_name, (chapterIdx ?: 0) + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            timeAgo(time),
            fontFamily = MonoFont(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun timeAgo(millis: Long): String {
    val m = (dev.haipham22.leechtext.util.nowMillis() - millis) / 60_000
    return when {
        m < 1 -> stringResource(Res.string.time_just_now)
        m < 60 -> stringResource(Res.string.time_minutes_ago, m)
        m < 60 * 24 -> stringResource(Res.string.time_hours_ago, m / 60)
        else -> stringResource(Res.string.time_days_ago, m / (60 * 24))
    }
}

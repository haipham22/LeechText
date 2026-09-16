package dev.haipham22.leechtext.ui.components.molecules
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haipham22.leechtext.ui.theme.MonoFont

/**
 * Tile sách theo master "Tachidesk Light": cover 2:3 rounded-xl border subtle,
 * badge pill teal TOP-LEFT (mono), progress 4dp bottom edge, title 2 dòng —
 * không author, không card chrome. Cover là slot (CoverImage ở jvmMain).
 */
@Composable
fun BookCoverTile(
    title: String,
    cover: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pendingCount: Int = 0,
    progress: (() -> Float)? = null,
    subtitle: String? = null,
    onClick: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                ).clickable(onClick = onClick),
        ) {
            cover()

            // Badge pill teal top-start — mono, như master
            if (pendingCount > 0) {
                Text(
                    "$pendingCount",
                    fontFamily = MonoFont(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }

            // Progress 4dp bottom edge — track mờ, fill teal
            if (progress != null) {
                LinearProgressIndicator(
                    progress = progress,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                fontFamily = MonoFont(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

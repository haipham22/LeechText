package dev.haipham22.leechtext.ui.components.molecules
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haipham22.leechtext.ui.model.RailItem
import dev.haipham22.leechtext.ui.theme.DisplayFont
import dev.haipham22.leechtext.ui.theme.MonoFont

/**
 * Navigation rail icon-only theo pattern Slack/VS Code — label tiếng Việt quá dài
 * cho rail 80dp, icon-only sạch hơn. Active = bar 4px teal trái + icon tile teal/10%
 * + icon teal. Badge teal circle mono.
 */
@Composable
fun AppRail(
    items: List<RailItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bottomItem: RailItem? = null,
    onBottomClick: () -> Unit = {},
) {
    Box(
        modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.fillMaxHeight()) {
            // Brand T — 56dp cao, border dưới
            Box(
                Modifier.fillMaxWidth().height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "L",
                    fontFamily = DisplayFont(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            )
            // Nav items — icon only, centered vertically
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items.forEachIndexed { i, item ->
                    RailButton(item, i == selected) { onSelect(i) }
                }
                if (bottomItem != null) {
                    Spacer(Modifier.weight(1f))
                    RailButton(bottomItem, active = false, onClick = onBottomClick)
                }
            }
        }
        // Border phải 1px
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        )
    }
}

@Composable
private fun RailButton(
    item: RailItem,
    active: Boolean,
    onClick: () -> Unit,
) {
    val teal = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) teal.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = if (active) teal else muted,
            modifier = Modifier.size(22.dp),
        )
        if (item.badge > 0) {
            Text(
                "${item.badge}",
                color = MaterialTheme.colorScheme.onPrimary,
                fontFamily = MonoFont(),
                fontSize = 9.sp,
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(teal)
                    .padding(horizontal = 4.dp),
            )
        }
        // Bar 3px teal bên trái khi active — inset trong tile
        if (active) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100))
                    .background(teal),
            )
        }
    }
}

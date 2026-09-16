package dev.haipham22.leechtext.ui.components.molecules
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haipham22.leechtext.ui.model.RailItem
import dev.haipham22.leechtext.ui.theme.MonoFont

/**
 * Bottom navigation mobile theo master Tachiyomi: 5 tab, active teal,
 * label mono caps nhỏ. Mirror của AppRail (desktop).
 * iOS: pill nổi kiểu iOS 26 (liquid-glass-ish). Android: NavigationBar chuẩn M3
 * full-width sát đáy — convention 2 nền khác nhau.
 */
@Composable
fun AppBottomBar(
    items: List<RailItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (dev.haipham22.leechtext.ui.IS_IOS) {
        IosFloatingBottomBar(items, selected, onSelect, modifier)
    } else {
        NavigationBar(modifier = modifier) {
            items.forEachIndexed { i, _ -> BottomNavItem(items, i, selected, onSelect) }
        }
    }
}

/** iOS 26 vibe: pill nổi, nền translucent, hairline viền. Đáy: inset - 24dp
 * nhưng không dưới 8dp → iPhone home indicator (34pt) pill cách mép ~10dp. */
@Composable
private fun IosFloatingBottomBar(
    items: List<RailItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val insetBottom = WindowInsets.navigationBars.getBottom(density)
    val pillBottom = with(density) {
        ((insetBottom - 24.dp.toPx()).coerceAtLeast(8.dp.toPx())).toDp()
    }
    androidx.compose.foundation.layout.Box(
        modifier
            .padding(bottom = pillBottom)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 8.dp,
        ) {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                items.forEachIndexed { i, _ -> BottomNavItem(items, i, selected, onSelect) }
            }
        }
    }
}

/** 1 tab: icon (badge nếu có) + label mono + màu active teal. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomNavItem(
    items: List<RailItem>,
    index: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val item = items[index]
    NavigationBarItem(
        selected = index == selected,
        onClick = { onSelect(index) },
        icon = {
            if (item.badge > 0) {
                BadgedBox(badge = { Badge { BadgeCount(item.badge) } }) {
                    Icon(item.icon, contentDescription = item.label)
                }
            } else {
                Icon(item.icon, contentDescription = item.label)
            }
        },
        label = { RailLabel(item.label) },
        colors =
        androidx.compose.material3.NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun BadgeCount(count: Int) {
    Text(
        "$count",
        fontFamily = MonoFont(),
        fontSize = 10.sp,
    )
}

@Composable
private fun RailLabel(label: String) {
    Text(
        label,
        fontFamily = MonoFont(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.08.sp,
        maxLines = 1,
    )
}

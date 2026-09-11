package anhiutangerine.prettiembee.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import anhiutangerine.prettiembee.R
import kotlin.math.roundToInt

data class BottomBarItem(
    val icon: ImageVector,
    val labelRes: Int
)

@Composable
fun defaultNavigationItems(): List<BottomBarItem> = listOf(
    BottomBarItem(Icons.Rounded.Home, R.string.nav_home),
    BottomBarItem(Icons.Rounded.Palette, R.string.nav_store),
    BottomBarItem(Icons.Rounded.Settings, R.string.nav_settings)
)

@Composable
fun FloatingBottomBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<BottomBarItem> = emptyList(),
    visible: Boolean = true
) {
    val navItems = if (items.isNotEmpty()) items else defaultNavigationItems()
    val density = LocalDensity.current
    val itemSize = 56.dp
    val itemSpacing = 6.dp
    val containerPadding = 6.dp

    val itemSizePx = with(density) { itemSize.toPx() }
    val itemSpacingPx = with(density) { itemSpacing.toPx() }

    val navBarWidth = (itemSize * navItems.size) + (itemSpacing * (navItems.size - 1)) + (containerPadding * 2)

    val animatedSelectedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat().coerceIn(0f, (navItems.size - 1).toFloat()),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bottom_bar_indicator"
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                tonalElevation = 3.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                ),
                modifier = Modifier.width(navBarWidth)
            ) {
                Box(
                    modifier = Modifier.padding(containerPadding)
                ) {
                    // Animated sliding indicator pill (KittiSU style)
                    val indicatorOffset = (itemSizePx + itemSpacingPx) * animatedSelectedIndex
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = indicatorOffset.roundToInt(), y = 0) }
                            .size(itemSize)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                    )

                    // 3 Icon Items Row (Icon-only, no text)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isSelected = index == selectedIndex
                            val interactionSource = remember { MutableInteractionSource() }

                            Box(
                                modifier = Modifier
                                    .size(itemSize)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onItemSelected(index) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.labelRes),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

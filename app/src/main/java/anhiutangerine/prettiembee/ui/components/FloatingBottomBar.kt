package anhiutangerine.prettiembee.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class BottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val NavigationItems = listOf(
    BottomBarItem(
        label = "Kho theme",
        icon = Icons.Rounded.Palette,
        selectedIcon = Icons.Rounded.Palette
    ),
    BottomBarItem(
        label = "Hệ thống",
        icon = Icons.Rounded.Tune,
        selectedIcon = Icons.Rounded.Tune
    )
)

@Composable
fun FloatingBottomBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<BottomBarItem> = NavigationItems,
    visible: Boolean = true
) {
    val density = LocalDensity.current
    val itemWidth = 118.dp
    val itemHeight = 44.dp
    val itemSpacing = 6.dp
    val containerPadding = 6.dp

    val itemWidthPx = with(density) { itemWidth.toPx() }
    val itemSpacingPx = with(density) { itemSpacing.toPx() }

    val animatedSelectedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            ) {
                Box(
                    modifier = Modifier.padding(containerPadding)
                ) {
                    // Animated sliding indicator pill
                    val indicatorOffset = (itemWidthPx + itemSpacingPx) * animatedSelectedIndex
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = indicatorOffset.roundToInt(), y = 0) }
                            .width(itemWidth)
                            .height(itemHeight)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(18.dp)
                            )
                    )

                    // Tab Items Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEachIndexed { index, item ->
                            val isSelected = index == selectedIndex
                            val interactionSource = remember { MutableInteractionSource() }

                            Box(
                                modifier = Modifier
                                    .width(itemWidth)
                                    .height(itemHeight)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onItemSelected(index) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

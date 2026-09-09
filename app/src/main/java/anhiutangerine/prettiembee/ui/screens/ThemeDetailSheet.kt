package anhiutangerine.prettiembee.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.data.model.InjectConfig
import anhiutangerine.prettiembee.ui.components.SegmentedGroup
import anhiutangerine.prettiembee.ui.components.SegmentedItem
import anhiutangerine.prettiembee.ui.theme.SakuraAccent
import anhiutangerine.prettiembee.ui.theme.SakuraPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailSheet(
    theme: CommunityTheme,
    currentTargetUuid: String,
    currentTargetName: String,
    onOpenTargetPicker: () -> Unit,
    onStartInject: (InjectConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var usePriority by remember { mutableStateOf(false) }
    var autoLaunchMb by remember { mutableStateOf(true) }
    var autoDeeplink by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (theme.isCustomImport) Icons.Rounded.AutoAwesome else Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = theme.series,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (theme.supportsPriority) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SakuraAccent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Priority Support",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SakuraAccent
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${theme.description}\nTác giả: ${theme.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // TARGET MB THEME SELECTOR
            SegmentedGroup(
                title = "VỊ TRÍ ÁP DỤNG TRONG MB BANK"
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenTargetPicker),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTargetName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "UUID: $currentTargetUuid",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Đổi",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Change",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // OPTIONS GROUP
            SegmentedGroup(
                title = "TUỲ CHỌN NẠP THEME"
            ) {
                if (theme.supportsPriority) {
                    SegmentedItem(
                        title = "Chế độ Priority",
                        subtitle = "Tối ưu icon & màu sắc cho gói VIP Priority",
                        icon = Icons.Rounded.Star,
                        iconTint = SakuraAccent,
                        showDivider = true,
                        trailingContent = {
                            Switch(
                                checked = usePriority,
                                onCheckedChange = { usePriority = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SakuraPink,
                                    checkedTrackColor = SakuraPink.copy(alpha = 0.35f)
                                )
                            )
                        }
                    )
                }

                SegmentedItem(
                    title = "Mở trang Theme (Deeplink)",
                    subtitle = "Tự mở MB Bank để nhấn 'Áp dụng'",
                    icon = Icons.Rounded.AutoAwesome,
                    iconTint = SakuraPink,
                    showDivider = false,
                    trailingContent = {
                        Switch(
                            checked = autoDeeplink,
                            onCheckedChange = { autoDeeplink = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SakuraPink,
                                checkedTrackColor = SakuraPink.copy(alpha = 0.35f)
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // CTA APPLY BUTTON
            Button(
                onClick = {
                    onStartInject(
                        InjectConfig(
                            sourceTheme = theme,
                            targetUuid = currentTargetUuid,
                            usePriorityVariant = usePriority,
                            autoLaunchMb = autoLaunchMb,
                            autoLaunchDeeplink = autoDeeplink
                        )
                    )
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SakuraPink,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Áp Dụng Theme Ngay",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

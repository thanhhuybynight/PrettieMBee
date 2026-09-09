package anhiutangerine.prettiembee.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anhiutangerine.prettiembee.data.model.InstalledTheme
import anhiutangerine.prettiembee.data.model.MbStoreTheme
import anhiutangerine.prettiembee.ui.theme.HoneyAmber
import anhiutangerine.prettiembee.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetThemePickerBottomSheet(
    installedThemes: List<InstalledTheme>,
    storeThemes: List<MbStoreTheme>,
    selectedUuid: String,
    onSelectTarget: (String, String) -> Unit, // (uuid, displayName)
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(if (installedThemes.isNotEmpty()) 0 else 1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Chọn Theme Đích MB Bank",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Theme tuỳ chỉnh sẽ ghi đè vào gói theme này trong MB Bank",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Đã có trên máy (${installedThemes.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Kho MB Store (${storeThemes.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm theo tên hoặc UUID...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == 0) {
                    val filteredInstalled = installedThemes.filter {
                        val name = it.storeTheme?.displayName ?: it.uuid
                        name.contains(searchQuery, ignoreCase = true) || it.uuid.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredInstalled.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa phát hiện theme nào đã tải trong máy.\nHãy chuyển sang tab 'Kho MB Store'!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        items(filteredInstalled) { installed ->
                            val displayName = installed.storeTheme?.displayName ?: "Theme (${installed.uuid.take(8)}...)"
                            TargetItem(
                                title = displayName,
                                subtitle = "UUID: ${installed.uuid}",
                                isSelected = installed.uuid.equals(selectedUuid, ignoreCase = true),
                                badgeText = "Có sẵn (${installed.imageCount} ảnh)",
                                badgeColor = SuccessGreen,
                                onClick = {
                                    onSelectTarget(installed.uuid, displayName)
                                    onDismiss()
                                }
                            )
                        }
                    }
                } else {
                    val filteredStore = storeThemes.filter {
                        it.displayName.contains(searchQuery, ignoreCase = true) ||
                                it.uuid.contains(searchQuery, ignoreCase = true) ||
                                it.slug.contains(searchQuery, ignoreCase = true)
                    }

                    items(filteredStore) { store ->
                        val isInstalledOnDevice = installedThemes.any { it.uuid.equals(store.uuid, ignoreCase = true) }
                        TargetItem(
                            title = store.displayName,
                            subtitle = "UUID: ${store.uuid}",
                            isSelected = store.uuid.equals(selectedUuid, ignoreCase = true),
                            badgeText = if (isInstalledOnDevice) "Đã tải" else "MB Store",
                            badgeColor = if (isInstalledOnDevice) SuccessGreen else HoneyAmber,
                            onClick = {
                                onSelectTarget(store.uuid, store.displayName)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) HoneyAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = HoneyAmber
                )
            }
        }
    }
}

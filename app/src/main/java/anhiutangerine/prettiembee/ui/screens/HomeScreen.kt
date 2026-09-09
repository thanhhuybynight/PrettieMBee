package anhiutangerine.prettiembee.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anhiutangerine.prettiembee.R
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.data.model.InstalledTheme
import anhiutangerine.prettiembee.ui.components.FloatingBottomBar
import anhiutangerine.prettiembee.ui.components.SegmentedGroup
import anhiutangerine.prettiembee.ui.components.SegmentedItem
import anhiutangerine.prettiembee.ui.components.StatusCard
import anhiutangerine.prettiembee.ui.components.ThemeCard
import anhiutangerine.prettiembee.ui.theme.SakuraAccent
import anhiutangerine.prettiembee.ui.theme.SakuraPink
import anhiutangerine.prettiembee.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isRootGranted: Boolean,
    isMbInstalled: Boolean,
    targetPackage: String,
    installedThemeCount: Int,
    installedThemes: List<InstalledTheme>,
    communityThemes: List<CommunityTheme>,
    isThemeDownloaded: (CommunityTheme) -> Boolean,
    onRefreshStatus: () -> Unit,
    onChangePackage: (String) -> Unit,
    onSelectTheme: (CommunityTheme) -> Unit,
    onImportZip: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var searchQuery by remember { mutableStateOf("") }
    var showPackageDialog by remember { mutableStateOf(false) }
    var tempPackageInput by remember { mutableStateOf(targetPackage) }

    LaunchedEffect(targetPackage) {
        tempPackageInput = targetPackage
    }

    val categories = remember(communityThemes) {
        listOf("Tất cả") + communityThemes.map { it.series }.distinct()
    }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_theme.zip"
            onImportZip(uri, fileName)
        }
    }

    if (showPackageDialog) {
        AlertDialog(
            onDismissRequest = { showPackageDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Đổi gói MB Bank mục tiêu",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Mặc định là 'com.mbmobile'. Nếu bạn dùng ứng dụng kép (Dual App) hoặc bản Clone, hãy nhập package tương ứng:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempPackageInput,
                        onValueChange = { tempPackageInput = it.trim() },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChangePackage(tempPackageInput)
                        showPackageDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SakuraPink,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Lưu", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPackageDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Huỷ")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_prettiembee_logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "PrettieMBee",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedTab == 0) "Kho theme cộng đồng" else "Trạng thái & cấu hình",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        FilledTonalButton(
                            onClick = { zipPickerLauncher.launch("application/zip") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = "Import ZIP",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nạp ZIP",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        IconButton(onClick = onRefreshStatus) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            FloatingBottomBar(
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_content_transition"
        ) { tab ->
            when (tab) {
                0 -> {
                    // TAB 0: KHO THEME
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        // Search Bar
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "Tìm theme theo tên hoặc tác giả...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            )
                        }

                        // Category Filter Row
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(categories) { category ->
                                    val isSelected = category == selectedCategory
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = category },
                                        label = {
                                            Text(
                                                text = category,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }

                        // Filter Themes
                        val filteredThemes = communityThemes.filter { theme ->
                            val matchCategory = selectedCategory == "Tất cả" || theme.series == selectedCategory
                            val matchQuery = searchQuery.isBlank() ||
                                    theme.name.contains(searchQuery, ignoreCase = true) ||
                                    theme.author.contains(searchQuery, ignoreCase = true) ||
                                    theme.series.contains(searchQuery, ignoreCase = true)
                            matchCategory && matchQuery
                        }

                        // Section Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedCategory == "Tất cả") "Tất cả theme" else selectedCategory,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "${filteredThemes.size} theme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                        }

                        items(filteredThemes, key = { it.id }) { theme ->
                            ThemeCard(
                                theme = theme,
                                isDownloaded = isThemeDownloaded(theme),
                                onClick = { onSelectTheme(theme) }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: HỆ THỐNG & CÀI ĐẶT
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        // Hero Status Card
                        item {
                            StatusCard(
                                isRootGranted = isRootGranted,
                                isMbInstalled = isMbInstalled,
                                targetPackage = targetPackage,
                                installedThemeCount = installedThemeCount,
                                onRefresh = onRefreshStatus,
                                onEditPackage = { showPackageDialog = true }
                            )
                        }

                        // Configuration Group
                        item {
                            SegmentedGroup(
                                title = "Cấu hình MB Bank"
                            ) {
                                SegmentedItem(
                                    title = "Package mục tiêu",
                                    subtitle = targetPackage,
                                    icon = Icons.Rounded.Smartphone,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    onClick = { showPackageDialog = true },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    showDivider = true
                                )

                                SegmentedItem(
                                    title = "Trạng thái ứng dụng MB",
                                    subtitle = if (isMbInstalled) "Đã cài đặt trên thiết bị" else "Chưa phát hiện gói cài đặt",
                                    icon = Icons.Rounded.CheckCircle,
                                    iconTint = if (isMbInstalled) SuccessGreen else SakuraAccent,
                                    showDivider = true
                                )

                                SegmentedItem(
                                    title = "Quyền SuperUser (Root)",
                                    subtitle = if (isRootGranted) "Đã cấp quyền root thành công" else "Chưa cấp quyền root",
                                    icon = Icons.Rounded.Security,
                                    iconTint = if (isRootGranted) SuccessGreen else SakuraAccent,
                                    showDivider = false
                                )
                            }
                        }

                        // Installed Themes in Target Package Group
                        item {
                            SegmentedGroup(
                                title = "Theme đã phát hiện ($targetPackage)"
                            ) {
                                if (installedThemes.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Chưa phát hiện theme nào trong bộ nhớ app MB.\nHãy mở MB Bank > Tiện ích > Giao diện để tải ít nhất 1 theme!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    installedThemes.forEachIndexed { index, installed ->
                                        val displayName = installed.storeTheme?.displayName ?: "Theme (${installed.uuid.take(8)}...)"
                                        SegmentedItem(
                                            title = displayName,
                                            subtitle = "UUID: ${installed.uuid} (${installed.imageCount} files)",
                                            icon = Icons.Rounded.Palette,
                                            iconTint = MaterialTheme.colorScheme.primary,
                                            showDivider = index < installedThemes.lastIndex
                                        )
                                    }
                                }
                            }
                        }

                        // About Group
                        item {
                            SegmentedGroup(
                                title = "Thông tin ứng dụng"
                            ) {
                                SegmentedItem(
                                    title = "PrettieMBee",
                                    subtitle = "Phiên bản 1.0.0 • anhiutangerine.prettiembee",
                                    icon = Icons.Rounded.Info,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    showDivider = true
                                )

                                SegmentedItem(
                                    title = "Theme Data",
                                    subtitle = "Nguồn tài nguyên từ repo cộng đồng mbcp (disroot.org)",
                                    showDivider = true
                                )

                                SegmentedItem(
                                    title = "UI Aesthetic",
                                    subtitle = "Thiết kế lấy cảm hứng từ phong cách tối giản của KittiSU",
                                    showDivider = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

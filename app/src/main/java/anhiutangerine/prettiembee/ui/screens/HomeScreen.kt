package anhiutangerine.prettiembee.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anhiutangerine.prettiembee.BuildConfig
import anhiutangerine.prettiembee.R
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.data.model.InstalledTheme
import anhiutangerine.prettiembee.ui.components.FloatingBottomBar
import anhiutangerine.prettiembee.ui.components.SegmentedGroup
import anhiutangerine.prettiembee.ui.components.SegmentedItem
import anhiutangerine.prettiembee.ui.components.StatusCard
import anhiutangerine.prettiembee.ui.components.ThemeCard
import anhiutangerine.prettiembee.ui.theme.AppThemeAccent
import anhiutangerine.prettiembee.ui.theme.AppThemeMode
import anhiutangerine.prettiembee.ui.theme.ThemeConfig
import kotlin.math.abs

@Composable
fun rememberScrollConnection(
    isScrollingDown: MutableState<Boolean>,
    scrollOffset: MutableState<Float>,
    previousScrollOffset: MutableState<Float>,
    threshold: Float = 35f
): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = scrollOffset.value + delta
                scrollOffset.value = newOffset
                val scrollDelta = previousScrollOffset.value - newOffset

                if (abs(scrollDelta) > threshold) {
                    isScrollingDown.value = scrollDelta > 0
                    previousScrollOffset.value = newOffset
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                previousScrollOffset.value = scrollOffset.value
                return super.onPostFling(consumed, available)
            }
        }
    }
}

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
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var searchQuery by remember { mutableStateOf("") }
    var showPackageDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var tempPackageInput by remember { mutableStateOf(targetPackage) }

    // Scroll connection for floating navbar auto hide/show (KittiSU style)
    val isScrollingDown = remember { mutableStateOf(false) }
    val scrollOffset = remember { mutableFloatStateOf(0f) }
    val previousScrollOffset = remember { mutableFloatStateOf(0f) }
    val bottomBarScrollConnection = rememberScrollConnection(
        isScrollingDown = isScrollingDown,
        scrollOffset = scrollOffset,
        previousScrollOffset = previousScrollOffset
    )

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
                        containerColor = MaterialTheme.colorScheme.primary,
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

    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Chế độ giao diện",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = ThemeConfig.themeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    ThemeConfig.saveThemeMode(context, mode)
                                    showThemeModeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    ThemeConfig.saveThemeMode(context, mode)
                                    showThemeModeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeModeDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đóng")
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
                                text = "v${BuildConfig.VERSION_NAME} - ${BuildConfig.GIT_HASH}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    when (selectedTab) {
                        0, 2 -> {
                            IconButton(onClick = onRefreshStatus) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        1 -> {
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
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(bottomBarScrollConnection)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content_transition"
            ) { tab ->
                when (tab) {
                    0 -> {
                        // TAB 0: TRANG CHỦ (Home)
                        // Giao diện chỉ có status card và thông tin hệ thống
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                        ) {
                            item {
                                StatusCard(
                                    isRootGranted = isRootGranted,
                                    isMbInstalled = isMbInstalled
                                )
                            }

                            item {
                                SegmentedGroup(
                                    title = "Thông tin hệ thống"
                                ) {
                                    val mbVersion = remember(isMbInstalled, targetPackage) {
                                        if (!isMbInstalled) {
                                            "Chưa cài đặt"
                                        } else {
                                            try {
                                                val pInfo = context.packageManager.getPackageInfo(targetPackage, 0)
                                                pInfo.versionName ?: "Đã cài đặt"
                                            } catch (e: Exception) {
                                                "Đã cài đặt"
                                            }
                                        }
                                    }

                                    SegmentedItem(
                                        title = "Phiên bản MB Bank",
                                        subtitle = mbVersion,
                                        icon = Icons.Rounded.AccountBalance,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = "Phiên bản Android",
                                        subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                        icon = Icons.Rounded.Android,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        showDivider = true
                                    )

                                    val deviceModel = remember {
                                        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                                        val model = Build.MODEL
                                        if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
                                    }

                                    SegmentedItem(
                                        title = "Thiết bị",
                                        subtitle = deviceModel,
                                        icon = Icons.Rounded.PhoneAndroid,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = "Phiên bản PrettieMBee",
                                        subtitle = "v${BuildConfig.VERSION_NAME} - ${BuildConfig.GIT_HASH}",
                                        icon = Icons.Rounded.Info,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        showDivider = false
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: KHO THEME (Themes)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
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

                    2 -> {
                        // TAB 2: CÀI ĐẶT (Settings)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                        ) {
                            // MB Bank Configuration Group
                            item {
                                SegmentedGroup(
                                    title = "Cấu hình MB Bank"
                                ) {
                                    SegmentedItem(
                                        title = "Gói ứng dụng mục tiêu",
                                        subtitle = targetPackage,
                                        icon = Icons.Rounded.Smartphone,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = { showPackageDialog = true },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Edit Package",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        showDivider = false
                                    )
                                }
                            }

                            // Appearance & Customization (KittiSU style)
                            item {
                                SegmentedGroup(
                                    title = "Giao diện & Chủ đề"
                                ) {
                                    SegmentedItem(
                                        title = "Chế độ nền",
                                        subtitle = ThemeConfig.themeMode.title,
                                        icon = when (ThemeConfig.themeMode) {
                                            AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                                            AppThemeMode.DARK -> Icons.Rounded.DarkMode
                                            AppThemeMode.SYSTEM -> Icons.Rounded.Contrast
                                        },
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = { showThemeModeDialog = true },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = "Select Mode",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = "Màu chủ đạo",
                                        subtitle = ThemeConfig.themeAccent.title,
                                        icon = Icons.Rounded.Palette,
                                        iconTint = ThemeConfig.themeAccent.previewColor,
                                        trailingContent = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AppThemeAccent.entries.forEach { accent ->
                                                    val isSelected = ThemeConfig.themeAccent == accent
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(accent.previewColor)
                                                            .clickable {
                                                                ThemeConfig.saveThemeAccent(context, accent)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = "Selected",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        showDivider = false
                                    )
                                }
                            }

                            // Data & Utilities Group
                            item {
                                SegmentedGroup(
                                    title = "Hệ thống & Dữ liệu"
                                ) {
                                    SegmentedItem(
                                        title = "Làm mới trạng thái",
                                        subtitle = "Quét lại quyền root và kiểm tra ứng dụng MB",
                                        icon = Icons.Rounded.Refresh,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = onRefreshStatus,
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = "Refresh",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = "Nhập theme từ file ZIP",
                                        subtitle = "Nạp theme tuỳ chỉnh thủ công từ bộ nhớ máy",
                                        icon = Icons.Rounded.FolderOpen,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = { zipPickerLauncher.launch("application/zip") },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = "Import",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = "Thư mục lưu trữ theme",
                                        subtitle = "/data/data/anhiutangerine.prettiembee/files/themes",
                                        icon = Icons.Rounded.Storage,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        showDivider = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Bottom Bar truly floating over content dock-style
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FloatingBottomBar(
                    selectedIndex = selectedTab,
                    onItemSelected = {
                        selectedTab = it
                        isScrollingDown.value = false
                    },
                    visible = !isScrollingDown.value
                )
            }
        }
    }
}

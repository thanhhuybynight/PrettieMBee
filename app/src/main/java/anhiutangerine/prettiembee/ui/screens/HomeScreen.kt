package anhiutangerine.prettiembee.ui.screens

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wallpaper
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
import androidx.compose.ui.res.stringResource
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
import kotlin.math.roundToInt

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
    onTogglePin: (CommunityTheme) -> Unit,
    onDeleteDownloaded: (CommunityTheme) -> Unit,
    onImportZip: (Uri, String) -> Unit,
    onResetThemes: (suspend () -> Result<Unit>)? = null,
    onLanguageChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val allCategoryKey = "ALL"
    val allCategoryLabel = stringResource(R.string.store_all)
    var selectedCategory by remember { mutableStateOf(allCategoryKey) }
    var searchQuery by remember { mutableStateOf("") }
    var showPackageDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showDpiDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var isResettingThemes by remember { mutableStateOf(false) }
    var tempPackageInput by remember { mutableStateOf(targetPackage) }
    var tempDpiInput by remember { mutableIntStateOf(ThemeConfig.appDpi) }

    // Image Pickers for Status Card & App Background
    val statusCardBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            ThemeConfig.saveStatusCardBackground(context, uri)
            Toast.makeText(context, context.getString(R.string.toast_status_bg_set), Toast.LENGTH_SHORT).show()
        }
    }

    val appBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            ThemeConfig.saveAppBackground(context, uri)
            Toast.makeText(context, context.getString(R.string.toast_app_bg_set), Toast.LENGTH_SHORT).show()
        }
    }

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
        listOf(allCategoryKey) + communityThemes.map { it.series }.distinct()
    }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_theme.zip"
            onImportZip(uri, fileName)
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    listOf("en" to R.string.language_english, "vi" to R.string.language_vietnamese).forEach { (tag, labelRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showLanguageDialog = false
                                    if (ThemeConfig.appLanguageTag != tag) onLanguageChange(tag)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = ThemeConfig.appLanguageTag == tag,
                                onClick = {
                                    showLanguageDialog = false
                                    if (ThemeConfig.appLanguageTag != tag) onLanguageChange(tag)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showPackageDialog) {
        AlertDialog(
            onDismissRequest = { showPackageDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.package_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.default_package_hint),
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
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPackageDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
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
                    text = stringResource(R.string.theme_mode_dialog_title),
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
                                text = stringResource(mode.titleRes),
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
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    if (showDpiDialog) {
        AlertDialog(
            onDismissRequest = { showDpiDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.dpi_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.dpi_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val presets = listOf(
                        0 to stringResource(R.string.dpi_value),
                        320 to stringResource(R.string.dpi_preset_small),
                        380 to stringResource(R.string.dpi_preset_default),
                        420 to stringResource(R.string.dpi_preset_medium),
                        480 to stringResource(R.string.dpi_preset_large)
                    )

                    presets.forEach { (presetDpi, label) ->
                        val isSelected = tempDpiInput == presetDpi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { tempDpiInput = presetDpi }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { tempDpiInput = presetDpi },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dpi_custom),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (tempDpiInput <= 0) stringResource(R.string.dpi_value) else stringResource(R.string.dpi_value_named, tempDpiInput),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = if (tempDpiInput <= 0) 380f else tempDpiInput.toFloat(),
                        onValueChange = { tempDpiInput = it.roundToInt() },
                        valueRange = 280f..560f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ThemeConfig.saveAppDpi(context, tempDpiInput)
                        showDpiDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.action_apply), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDpiDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResettingThemes) showResetConfirmDialog = false
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.reset_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onResetThemes != null) {
                            coroutineScope.launch {
                                isResettingThemes = true
                                val res = onResetThemes()
                                isResettingThemes = false
                                showResetConfirmDialog = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, context.getString(R.string.reset_success), Toast.LENGTH_LONG).show()
                                    onRefreshStatus()
                                } else {
                                    val err = res.exceptionOrNull()?.message ?: context.getString(R.string.operation_failed)
                                    Toast.makeText(context, context.getString(R.string.error_generic, err), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    enabled = !isResettingThemes
                ) {
                    if (isResettingThemes) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.action_reset_themes), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isResettingThemes
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_prettiembee_logo),
                                        contentDescription = stringResource(R.string.home_title),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.home_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "v${BuildConfig.VERSION_NAME} - ${BuildConfig.GIT_HASH}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshStatus) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (ThemeConfig.appBackgroundUri != null) Color.Transparent else MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = if (ThemeConfig.appBackgroundUri != null) Color.Transparent else MaterialTheme.colorScheme.background,
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
                                    isMbInstalled = isMbInstalled,
                                    appliedNewTheme = ThemeConfig.appliedNewThemeName,
                                    appliedOriginalTheme = ThemeConfig.appliedOriginalThemeName,
                                    appliedIsPriority = ThemeConfig.appliedIsPriority,
                                    appliedThemeId = ThemeConfig.appliedThemeId,
                                    backgroundUri = ThemeConfig.statusCardBackgroundUri,
                                    cardAlpha = ThemeConfig.cardAlpha
                                )
                            }

                            item {
                                SegmentedGroup(
                                    title = stringResource(R.string.home_system_info)
                                ) {
                                    val mbVersion = remember(isMbInstalled, targetPackage) {
                                        if (!isMbInstalled) {
                                            context.getString(R.string.status_mb_missing)
                                        } else {
                                            try {
                                                val pInfo = context.packageManager.getPackageInfo(targetPackage, 0)
                                                pInfo.versionName ?: context.getString(R.string.status_theme_installed)
                                            } catch (e: Exception) {
                                                context.getString(R.string.status_theme_installed)
                                            }
                                        }
                                    }

                                    SegmentedItem(
                                        title = stringResource(R.string.home_mb_version),
                                        subtitle = mbVersion,
                                        icon = Icons.Rounded.AccountBalance,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = stringResource(R.string.home_android_version),
                                        subtitle = stringResource(R.string.home_android_version_value, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
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
                                        title = stringResource(R.string.home_device),
                                        subtitle = deviceModel,
                                        icon = Icons.Rounded.PhoneAndroid,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = stringResource(R.string.home_app_version),
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
                                            text = stringResource(R.string.store_search_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = stringResource(R.string.store_search),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = stringResource(R.string.store_clear),
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
                                                    text = if (category == allCategoryKey) allCategoryLabel else category,
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

                            // Filter Themes, then float pinned to top
                            val filteredThemes = communityThemes
                                .filter { theme ->
                                    val matchCategory = selectedCategory == allCategoryKey || theme.series == selectedCategory
                                    val matchQuery = searchQuery.isBlank() ||
                                            theme.name.contains(searchQuery, ignoreCase = true) ||
                                            theme.author.contains(searchQuery, ignoreCase = true) ||
                                            theme.series.contains(searchQuery, ignoreCase = true)
                                    matchCategory && matchQuery
                                }
                                .sortedByDescending { theme ->
                                    if (ThemeConfig.isPinnedTheme(theme.id)) 1 else 0
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
                                        text = if (selectedCategory == allCategoryKey) allCategoryLabel else selectedCategory,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.store_theme_count, filteredThemes.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        )
                                        IconButton(
                                            onClick = { onRefreshStatus() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = stringResource(R.string.store_sync_github),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            items(filteredThemes, key = { it.id }) { theme ->
                                ThemeCard(
                                    theme = theme,
                                    isDownloaded = isThemeDownloaded(theme),
                                    isPinned = ThemeConfig.isPinnedTheme(theme.id),
                                    onClick = { onSelectTheme(theme) },
                                    onTogglePin = { onTogglePin(theme) },
                                    onDeleteDownloaded = { onDeleteDownloaded(theme) }
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
                                    title = stringResource(R.string.settings_mb_config)
                                ) {
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_target_package),
                                        subtitle = targetPackage,
                                        icon = Icons.Rounded.Smartphone,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = { showPackageDialog = true },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = stringResource(R.string.settings_target_package),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        showDivider = ThemeConfig.appliedNewThemeName != null
                                    )

                                    if (ThemeConfig.appliedNewThemeName != null) {
                                        val prioText = if (ThemeConfig.appliedIsPriority) " - Prio" else ""
                                        SegmentedItem(
                                            title = stringResource(R.string.settings_active_theme),
                                            subtitle = "${ThemeConfig.appliedNewThemeName} - ${ThemeConfig.appliedOriginalThemeName}$prioText",
                                            icon = Icons.Rounded.Check,
                                            iconTint = MaterialTheme.colorScheme.primary,
                                            trailingContent = {
                                                TextButton(
                                                    onClick = { ThemeConfig.clearAppliedTheme(context) }
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.action_reset),
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            },
                                            showDivider = false
                                        )
                                    }
                                }
                            }

                            // Appearance & Customization (KittiSU style)
                            item {
                                SegmentedGroup(
                                    title = stringResource(R.string.settings_ui_theme)
                                ) {
                                    // 1. Chế độ nền
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_theme_mode),
                                        subtitle = stringResource(ThemeConfig.themeMode.titleRes),
                                        icon = when (ThemeConfig.themeMode) {
                                            AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                                            AppThemeMode.MATERIAL_DARK -> Icons.Rounded.DarkMode
                                            AppThemeMode.OLED_DARK -> Icons.Rounded.Contrast
                                        },
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = { showThemeModeDialog = true },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = stringResource(R.string.settings_theme_mode),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        showDivider = true
                                    )

                                    // 2. Màu chủ đạo
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_accent_color),
                                        subtitle = if (ThemeConfig.useBackgroundSeedColor && ThemeConfig.extractedSeedColor != null) stringResource(R.string.accent_seed_active) else stringResource(ThemeConfig.themeAccent.titleRes),
                                        icon = Icons.Rounded.Palette,
                                        iconTint = if (ThemeConfig.useBackgroundSeedColor && ThemeConfig.extractedSeedColor != null) ThemeConfig.extractedSeedColor!! else ThemeConfig.themeAccent.previewColor,
                                        trailingContent = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AppThemeAccent.entries.forEach { accent ->
                                                    val isSelected = !ThemeConfig.useBackgroundSeedColor && ThemeConfig.themeAccent == accent
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(accent.previewColor)
                                                            .clickable {
                                                                ThemeConfig.saveUseBackgroundSeedColor(context, false)
                                                                ThemeConfig.saveThemeAccent(context, accent)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = stringResource(R.string.action_apply),
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        showDivider = true
                                    )

                                    // 3. Lấy màu từ hình nền (Pick color from background)
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_seed_color),
                                        subtitle = if (ThemeConfig.appBackgroundUri != null) stringResource(R.string.settings_seed_color_on) else stringResource(R.string.settings_seed_color_off),
                                        icon = Icons.Rounded.FormatColorFill,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        trailingContent = {
                                            Switch(
                                                checked = ThemeConfig.useBackgroundSeedColor,
                                                onCheckedChange = { isChecked ->
                                                    ThemeConfig.saveUseBackgroundSeedColor(context, isChecked)
                                                },
                                                enabled = ThemeConfig.appBackgroundUri != null
                                            )
                                        },
                                        showDivider = true
                                    )

                                    // 4. Nền Status Card (Custom background for status card)
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_status_card_bg),
                                        subtitle = if (ThemeConfig.statusCardBackgroundUri != null) stringResource(R.string.settings_status_card_bg_set) else stringResource(R.string.settings_status_card_bg_none),
                                        icon = Icons.Rounded.Wallpaper,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        trailingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                FilledTonalButton(
                                                    onClick = { statusCardBgPicker.launch("image/*") },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (ThemeConfig.statusCardBackgroundUri != null) stringResource(R.string.action_change) else stringResource(R.string.action_choose_image),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                if (ThemeConfig.statusCardBackgroundUri != null) {
                                                    IconButton(
                                                        onClick = {
                                                            ThemeConfig.saveStatusCardBackground(context, null)
                                                            Toast.makeText(context, context.getString(R.string.toast_status_bg_removed), Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = stringResource(R.string.action_delete),
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        showDivider = true
                                    )

                                    // 5. Nền toàn ứng dụng (Full screen app background)
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_app_bg),
                                        subtitle = if (ThemeConfig.appBackgroundUri != null) stringResource(R.string.settings_app_bg_set) else stringResource(R.string.settings_app_bg_none),
                                        icon = Icons.Rounded.Wallpaper,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        trailingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                FilledTonalButton(
                                                    onClick = { appBgPicker.launch("image/*") },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (ThemeConfig.appBackgroundUri != null) stringResource(R.string.action_change) else stringResource(R.string.action_choose_image),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                if (ThemeConfig.appBackgroundUri != null) {
                                                    IconButton(
                                                        onClick = {
                                                            ThemeConfig.saveAppBackground(context, null)
                                                            Toast.makeText(context, context.getString(R.string.toast_app_bg_removed), Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = stringResource(R.string.action_delete),
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        showDivider = true
                                    )

                                    // 6. Background Darkness Adjustment (chỉ hiện khi có nền app)
                                    if (ThemeConfig.appBackgroundUri != null) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.BrightnessMedium,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Column {
                                                        Text(
                                                            text = stringResource(R.string.settings_bg_dim),
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = stringResource(R.string.settings_bg_dim_desc),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${(ThemeConfig.backgroundDim * 100).roundToInt()}%",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Slider(
                                                value = ThemeConfig.backgroundDim,
                                                onValueChange = { ThemeConfig.backgroundDim = it.coerceIn(0f, 0.9f) },
                                                onValueChangeFinished = { ThemeConfig.saveBackgroundDim(context, ThemeConfig.backgroundDim) },
                                                valueRange = 0f..0.9f
                                            )
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            thickness = 0.8.dp
                                        )
                                    }

                                    // 7. Card Transparency (Card Alpha)
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Opacity,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = stringResource(R.string.settings_card_alpha),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.settings_card_alpha_desc),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${(ThemeConfig.cardAlpha * 100).roundToInt()}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = ThemeConfig.cardAlpha,
                                            onValueChange = { ThemeConfig.cardAlpha = it.coerceIn(0.1f, 1f) },
                                            onValueChangeFinished = { ThemeConfig.saveCardAlpha(context, ThemeConfig.cardAlpha) },
                                            valueRange = 0.1f..1f
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                        thickness = 0.8.dp
                                    )

                                    // 8. DPI ứng dụng
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_app_dpi),
                                        subtitle = if (ThemeConfig.appDpi <= 0) stringResource(R.string.dpi_value) else "${ThemeConfig.appDpi} DPI",
                                        icon = Icons.Rounded.AspectRatio,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            tempDpiInput = ThemeConfig.appDpi
                                            showDpiDialog = true
                                        },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = stringResource(R.string.settings_app_dpi),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        showDivider = false
                                    )
                                }
                            }

                            // Language
                            item {
                                SegmentedGroup(
                                    title = stringResource(R.string.settings_language)
                                ) {
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_language),
                                        subtitle = stringResource(R.string.settings_language_desc),
                                        icon = Icons.Rounded.Language,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = { showLanguageDialog = true },
                                        trailingContent = {
                                            Text(
                                                text = if (ThemeConfig.appLanguageTag == "vi") {
                                                    stringResource(R.string.language_vietnamese)
                                                } else {
                                                    stringResource(R.string.language_english)
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = stringResource(R.string.settings_language),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        showDivider = false
                                    )
                                }
                            }

                            // Data & Utilities Group
                            item {
                                SegmentedGroup(
                                    title = stringResource(R.string.settings_system_data)
                                ) {
                                    SegmentedItem(
                                        title = stringResource(R.string.settings_refresh_status),
                                        subtitle = stringResource(R.string.settings_refresh_status_desc),
                                        icon = Icons.Rounded.Refresh,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = onRefreshStatus,
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = stringResource(R.string.action_refresh),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = stringResource(R.string.settings_theme_folder),
                                        subtitle = context.filesDir.resolve("themes").absolutePath,
                                        icon = Icons.Rounded.Storage,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        showDivider = true
                                    )

                                    SegmentedItem(
                                        title = stringResource(R.string.settings_reset_themes),
                                        subtitle = stringResource(R.string.settings_reset_themes_desc),
                                        icon = Icons.Rounded.DeleteForever,
                                        iconTint = MaterialTheme.colorScheme.error,
                                        onClick = {
                                            if (!isRootGranted) {
                                                Toast.makeText(context, context.getString(R.string.reset_needs_root), Toast.LENGTH_SHORT).show()
                                            } else {
                                                showResetConfirmDialog = true
                                            }
                                        },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = stringResource(R.string.action_delete),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                            )
                                        },
                                        showDivider = false
                                    )
                                }
                            }
                        }
                    }
                }

            // Floating ZIP Import Button above navbar (Kho theme tab only)
            AnimatedVisibility(
                visible = selectedTab == 1 && !isScrollingDown.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 90.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { zipPickerLauncher.launch("application/zip") },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = stringResource(R.string.store_load_zip)
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.store_load_zip),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                )
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

}

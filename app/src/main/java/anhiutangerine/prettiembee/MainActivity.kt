package anhiutangerine.prettiembee

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.data.model.InjectConfig
import anhiutangerine.prettiembee.data.model.InjectResult
import anhiutangerine.prettiembee.data.model.InstalledTheme
import anhiutangerine.prettiembee.data.model.MbStoreTheme
import anhiutangerine.prettiembee.data.repository.RootRepository
import anhiutangerine.prettiembee.data.repository.ThemeRepository
import anhiutangerine.prettiembee.ui.screens.FlashScreen
import anhiutangerine.prettiembee.ui.screens.FlashScreenConstants
import anhiutangerine.prettiembee.ui.screens.FlashingStatus
import anhiutangerine.prettiembee.ui.components.TargetThemePickerBottomSheet
import anhiutangerine.prettiembee.ui.screens.HomeScreen
import anhiutangerine.prettiembee.ui.screens.ThemeDetailSheet
import anhiutangerine.prettiembee.ui.theme.PrettieMBeeTheme
import anhiutangerine.prettiembee.ui.theme.ThemeConfig
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var rootRepository: RootRepository
    private lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeConfig.load(applicationContext)
        rootRepository = RootRepository(applicationContext)
        themeRepository = ThemeRepository(applicationContext)

        setContent {
            PrettieMBeeTheme {
                val coroutineScope = rememberCoroutineScope()

                var isRootGranted by remember { mutableStateOf(false) }
                var isMbInstalled by remember { mutableStateOf(false) }
                var targetPackage by remember { mutableStateOf(rootRepository.targetPackage) }
                var installedThemes by remember { mutableStateOf<List<InstalledTheme>>(emptyList()) }
                var communityThemes by remember { mutableStateOf<List<CommunityTheme>>(emptyList()) }
                var storeThemes by remember { mutableStateOf<List<MbStoreTheme>>(emptyList()) }

                var selectedThemeForDetail by remember { mutableStateOf<CommunityTheme?>(null) }
                var currentTargetUuid by remember { mutableStateOf("aa3cb89a-8325-41b4-b59b-dfaea086cf80") }
                var currentTargetName by remember { mutableStateOf("Cánh Én Mùa Xuân") }

                var isTargetPickerOpen by remember { mutableStateOf(false) }

                // FlashScreen state
                var isFlashScreenOpen by remember { mutableStateOf(false) }
                var activeInstallConfig by remember { mutableStateOf<InjectConfig?>(null) }
                var activeTargetName by remember { mutableStateOf("") }
                var flashingStatus by remember { mutableStateOf(FlashingStatus.FLASHING) }
                var flashFailedReason by remember { mutableStateOf<String?>(null) }
                val injectLogs = remember { mutableStateListOf<String>() }

                // Function to refresh state
                val refreshAll = {
                    coroutineScope.launch {
                        rootRepository.targetPackage = targetPackage
                        isRootGranted = rootRepository.isRootAvailable()
                        isMbInstalled = rootRepository.isMbInstalled()
                        val loadedStore = themeRepository.getStoreThemes()
                        storeThemes = loadedStore
                        communityThemes = themeRepository.getCommunityThemes()

                        if (isRootGranted) {
                            installedThemes = rootRepository.getInstalledThemes(loadedStore)
                            // Auto select first installed theme as default target if available
                            installedThemes.firstOrNull()?.let { first ->
                                currentTargetUuid = first.uuid
                                currentTargetName = first.storeTheme?.displayName ?: "Theme (${first.uuid.take(8)}...)"
                            }
                        }
                    }
                }

                LaunchedEffect(targetPackage) {
                    refreshAll()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isFlashScreenOpen && activeInstallConfig != null) {
                        FlashScreen(
                            config = activeInstallConfig!!,
                            currentTargetName = activeTargetName,
                            logs = injectLogs,
                            status = flashingStatus,
                            failedReason = flashFailedReason,
                            onBack = { isFlashScreenOpen = false }
                        )
                    } else {
                        HomeScreen(
                            isRootGranted = isRootGranted,
                            isMbInstalled = isMbInstalled,
                            targetPackage = targetPackage,
                            installedThemeCount = installedThemes.size,
                            installedThemes = installedThemes,
                            communityThemes = communityThemes,
                            isThemeDownloaded = { theme -> themeRepository.isThemeDownloaded(theme) },
                            onRefreshStatus = {
                                Toast.makeText(applicationContext, "Đang làm mới dữ liệu và đồng bộ kho theme...", Toast.LENGTH_SHORT).show()
                                refreshAll()
                            },
                            onChangePackage = { newPkg ->
                                targetPackage = newPkg
                                rootRepository.targetPackage = newPkg
                            },
                            onSelectTheme = { theme ->
                                selectedThemeForDetail = theme
                                storeThemes.find { it.uuid.equals(theme.defaultTargetUuid, ignoreCase = true) }?.let { match ->
                                    currentTargetUuid = match.uuid
                                    currentTargetName = match.displayName
                                }
                            },
                            onImportZip = { uri, fileName ->
                                coroutineScope.launch {
                                    Toast.makeText(applicationContext, "Đang xử lý file ZIP...", Toast.LENGTH_SHORT).show()
                                    val res = themeRepository.importCustomZip(uri, fileName)
                                    if (res.isSuccess) {
                                        Toast.makeText(applicationContext, "Nạp theme thành công!", Toast.LENGTH_SHORT).show()
                                        communityThemes = themeRepository.getCommunityThemes()
                                        res.getOrNull()?.let { imported ->
                                            selectedThemeForDetail = imported
                                        }
                                    } else {
                                        Toast.makeText(applicationContext, "Lỗi: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onResetThemes = {
                                rootRepository.resetAllThemes()
                            }
                        )
                    }

                    // Theme Detail Bottom Sheet
                    selectedThemeForDetail?.let { theme ->
                        ThemeDetailSheet(
                            theme = theme,
                            currentTargetUuid = currentTargetUuid,
                            currentTargetName = currentTargetName,
                            onOpenTargetPicker = { isTargetPickerOpen = true },
                            onStartInject = { config ->
                                selectedThemeForDetail = null
                                activeInstallConfig = config
                                activeTargetName = currentTargetName
                                flashingStatus = FlashingStatus.FLASHING
                                flashFailedReason = null
                                injectLogs.clear()

                                val initialLines = FlashScreenConstants.createInitialLogs(
                                    config = config,
                                    currentTargetName = currentTargetName,
                                    targetPackage = targetPackage
                                )
                                injectLogs.addAll(initialLines)
                                isFlashScreenOpen = true

                                coroutineScope.launch {
                                    // Check if theme files exist locally
                                    var themeDir = themeRepository.getThemeDir(config.sourceTheme.id)
                                    if (!themeRepository.isThemeDownloaded(config.sourceTheme)) {
                                        injectLogs.add("- Đang tải theme từ máy chủ...")
                                        val dlRes = themeRepository.downloadTheme(config.sourceTheme) { progress ->
                                            val pct = (progress * 100).toInt()
                                            if (pct % 25 == 0) {
                                                injectLogs.add("- Tiến độ tải: $pct%")
                                            }
                                        }
                                        if (dlRes.isFailure) {
                                            val err = "Tải theme thất bại: ${dlRes.exceptionOrNull()?.message}"
                                            injectLogs.add("- $err")
                                            flashFailedReason = err
                                            flashingStatus = FlashingStatus.FAILED
                                            return@launch
                                        }
                                        themeDir = dlRes.getOrThrow()
                                        injectLogs.add("- Đã tải và giải nén theme.")
                                    }

                                    // Run Root Injection
                                    val res = rootRepository.injectTheme(config, themeDir) { logMsg ->
                                        injectLogs.add(logMsg)
                                    }

                                    if (res.isSuccess) {
                                        ThemeConfig.saveAppliedTheme(
                                            context = applicationContext,
                                            newTheme = config.sourceTheme.name,
                                            originalTheme = currentTargetName,
                                            isPriority = config.usePriorityVariant
                                        )
                                        flashingStatus = FlashingStatus.SUCCESS
                                        // Auto-launch MB Bank after successful install
                                        rootRepository.launchMbBank(
                                            useDeeplink = config.autoLaunchDeeplink,
                                            targetUuid = config.targetUuid
                                        )
                                    } else {
                                        val err = res.errorMessage ?: "Có lỗi xảy ra trong quá trình cài đặt"
                                        flashFailedReason = err
                                        flashingStatus = FlashingStatus.FAILED
                                    }
                                    refreshAll()
                                }
                            },
                            onDismiss = { selectedThemeForDetail = null }
                        )
                    }

                    // Target Theme Picker
                    if (isTargetPickerOpen) {
                        TargetThemePickerBottomSheet(
                            installedThemes = installedThemes,
                            storeThemes = storeThemes,
                            selectedUuid = currentTargetUuid,
                            onSelectTarget = { uuid, name ->
                                currentTargetUuid = uuid
                                currentTargetName = name
                            },
                            onDismiss = { isTargetPickerOpen = false }
                        )
                    }
                }
            }
        }
    }
}

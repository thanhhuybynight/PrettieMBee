package anhiutangerine.prettiembee

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import anhiutangerine.prettiembee.ui.components.InjectDialog
import anhiutangerine.prettiembee.ui.components.TargetThemePickerBottomSheet
import anhiutangerine.prettiembee.ui.screens.HomeScreen
import anhiutangerine.prettiembee.ui.screens.ThemeDetailSheet
import anhiutangerine.prettiembee.ui.theme.PrettieMBeeTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var rootRepository: RootRepository
    private lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                // Injection & Download state
                var isInjecting by remember { mutableStateOf(false) }
                var isInjectDialogOpen by remember { mutableStateOf(false) }
                val injectLogs = remember { mutableStateListOf<String>() }
                var injectResult by remember { mutableStateOf<InjectResult?>(null) }

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

                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        isRootGranted = isRootGranted,
                        isMbInstalled = isMbInstalled,
                        targetPackage = targetPackage,
                        installedThemeCount = installedThemes.size,
                        installedThemes = installedThemes,
                        communityThemes = communityThemes,
                        isThemeDownloaded = { theme -> themeRepository.isThemeDownloaded(theme) },
                        onRefreshStatus = { refreshAll() },
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
                        }
                    )

                    // Theme Detail Bottom Sheet
                    selectedThemeForDetail?.let { theme ->
                        ThemeDetailSheet(
                            theme = theme,
                            currentTargetUuid = currentTargetUuid,
                            currentTargetName = currentTargetName,
                            onOpenTargetPicker = { isTargetPickerOpen = true },
                            onStartInject = { config ->
                                selectedThemeForDetail = null
                                isInjectDialogOpen = true
                                isInjecting = true
                                injectLogs.clear()
                                injectResult = null

                                coroutineScope.launch {
                                    // Check if theme files exist locally
                                    var themeDir = themeRepository.getThemeDir(config.sourceTheme.id)
                                    if (!themeRepository.isThemeDownloaded(config.sourceTheme)) {
                                        injectLogs.add("📥 Theme chưa có sẵn trong máy. Đang tải từ máy chủ...")
                                        val dlRes = themeRepository.downloadTheme(config.sourceTheme) { progress ->
                                            if ((progress * 100).toInt() % 25 == 0) {
                                                injectLogs.add("⏳ Đang tải: ${(progress * 100).toInt()}%")
                                            }
                                        }
                                        if (dlRes.isFailure) {
                                            val err = "Tải theme thất bại: ${dlRes.exceptionOrNull()?.message}"
                                            injectLogs.add("❌ $err")
                                            injectResult = InjectResult(false, injectLogs, err)
                                            isInjecting = false
                                            return@launch
                                        }
                                        themeDir = dlRes.getOrThrow()
                                        injectLogs.add("✅ Đã tải và giải nén theme thành công!")
                                    }

                                    // Run Root Injection
                                    val res = rootRepository.injectTheme(config, themeDir) { logMsg ->
                                        injectLogs.add(logMsg)
                                    }
                                    injectResult = res
                                    isInjecting = false
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

                    // Injection Dialog with live logs
                    if (isInjectDialogOpen) {
                        InjectDialog(
                            isRunning = isInjecting,
                            logs = injectLogs,
                            result = injectResult,
                            onDismiss = { isInjectDialogOpen = false }
                        )
                    }
                }
            }
        }
    }
}

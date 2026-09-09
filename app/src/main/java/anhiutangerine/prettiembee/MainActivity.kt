package anhiutangerine.prettiembee

import android.os.Bundle
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
                var installedThemes by remember { mutableStateOf<List<InstalledTheme>>(emptyList()) }
                var communityThemes by remember { mutableStateOf<List<CommunityTheme>>(emptyList()) }
                var storeThemes by remember { mutableStateOf<List<MbStoreTheme>>(emptyList()) }

                var selectedThemeForDetail by remember { mutableStateOf<CommunityTheme?>(null) }
                var currentTargetUuid by remember { mutableStateOf("aa3cb89a-8325-41b4-b59b-dfaea086cf80") }
                var currentTargetName by remember { mutableStateOf("Cánh Én Mùa Xuân") }

                var isTargetPickerOpen by remember { mutableStateOf(false) }

                // Injection state
                var isInjecting by remember { mutableStateOf(false) }
                var isInjectDialogOpen by remember { mutableStateOf(false) }
                val injectLogs = remember { mutableStateListOf<String>() }
                var injectResult by remember { mutableStateOf<InjectResult?>(null) }

                // Function to refresh state
                val refreshAll = {
                    coroutineScope.launch {
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

                LaunchedEffect(Unit) {
                    refreshAll()
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        isRootGranted = isRootGranted,
                        isMbInstalled = isMbInstalled,
                        installedThemeCount = installedThemes.size,
                        communityThemes = communityThemes,
                        onRefreshStatus = { refreshAll() },
                        onSelectTheme = { theme ->
                            selectedThemeForDetail = theme
                            // If target theme matches default target UUID, set display name
                            storeThemes.find { it.uuid.equals(theme.defaultTargetUuid, ignoreCase = true) }?.let { match ->
                                currentTargetUuid = match.uuid
                                currentTargetName = match.displayName
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
                                    val res = rootRepository.injectTheme(config) { logMsg ->
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

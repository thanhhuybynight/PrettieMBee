package anhiutangerine.prettiembee

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import anhiutangerine.prettiembee.ui.components.TargetThemePickerBottomSheet
import anhiutangerine.prettiembee.ui.screens.FlashScreen
import anhiutangerine.prettiembee.ui.screens.HomeScreen
import anhiutangerine.prettiembee.ui.screens.ThemeDetailSheet
import anhiutangerine.prettiembee.ui.theme.PrettieMBeeTheme
import anhiutangerine.prettiembee.ui.theme.ThemeConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeConfig.load(applicationContext)
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(application) as T
        })[MainViewModel::class.java]

        setContent {
            PrettieMBeeTheme {
                LaunchedEffect(Unit) { model.refresh() }
                LaunchedEffect(model.message) {
                    model.message?.let {
                        Toast.makeText(applicationContext, it, Toast.LENGTH_LONG).show()
                        model.message = null
                    }
                }
                val config = model.activeInstallConfig
                if (model.isFlashScreenOpen && config != null) {
                    FlashScreen(
                        config = config,
                        currentTargetName = model.activeTargetName,
                        logs = model.injectLogs,
                        status = model.flashingStatus,
                        failedReason = model.flashFailedReason,
                        onBack = { model.isFlashScreenOpen = false }
                    )
                } else {
                    HomeScreen(
                        isRootGranted = model.isRootGranted,
                        isMbInstalled = model.isMbInstalled,
                        targetPackage = model.targetPackage,
                        installedThemeCount = model.installedThemes.size,
                        installedThemes = model.installedThemes,
                        communityThemes = model.communityThemes,
                        isThemeDownloaded = { it.id in model.downloadedThemeIds },
                        onRefreshStatus = { model.refresh(forceRefresh = true) },
                        onChangePackage = model::changePackage,
                        onSelectTheme = model::selectTheme,
                        onTogglePin = { ThemeConfig.togglePinnedTheme(applicationContext, it.id) },
                        onDeleteDownloaded = { model.deleteDownloaded(it) },
                        onImportZip = { uri, name -> model.importZip(uri, name) },
                        onResetThemes = { model.resetThemes() }
                    )
                }
                model.selectedTheme?.let { theme ->
                    ThemeDetailSheet(
                        theme = theme,
                        currentTargetUuid = model.currentTargetUuid,
                        currentTargetName = model.currentTargetName,
                        onOpenTargetPicker = { model.isTargetPickerOpen = true },
                        onStartInject = { model.startInject(it) },
                        onDismiss = { model.selectedTheme = null }
                    )
                }
                if (model.isTargetPickerOpen) {
                    TargetThemePickerBottomSheet(
                        installedThemes = model.installedThemes,
                        storeThemes = model.storeThemes,
                        selectedUuid = model.currentTargetUuid,
                        onSelectTarget = model::selectTarget,
                        onDismiss = { model.isTargetPickerOpen = false }
                    )
                }
            }
        }
    }
}

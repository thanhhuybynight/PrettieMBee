package anhiutangerine.prettiembee

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import anhiutangerine.prettiembee.data.model.*
import anhiutangerine.prettiembee.data.repository.RootRepository
import anhiutangerine.prettiembee.data.repository.TargetPreferences
import anhiutangerine.prettiembee.data.repository.ThemeRepository
import anhiutangerine.prettiembee.ui.screens.FlashScreenConstants
import anhiutangerine.prettiembee.ui.screens.FlashingStatus
import anhiutangerine.prettiembee.ui.theme.ThemeConfig
import kotlinx.coroutines.*

/** Owns operations across Activity recreation; repositories are fixed to a target package. */
class MainViewModel(
    application: Application,
    private val themeRepository: ThemeRepository = ThemeRepository(application),
    private val rootFactory: (Context, String) -> RootRepository = { context, pkg -> RootRepository(context, pkg) }
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication<Application>().applicationContext
    private val preferences = TargetPreferences(context)
    var targetPackage by mutableStateOf(preferences.targetPackage)
        private set
    private var rootRepository = rootFactory(context, targetPackage)
    private var refreshJob: Job? = null
    var isRootGranted by mutableStateOf(false)
        private set
    var isMbInstalled by mutableStateOf(false)
        private set
    var installedThemes by mutableStateOf<List<InstalledTheme>>(emptyList())
        private set
    var communityThemes by mutableStateOf<List<CommunityTheme>>(emptyList())
        private set
    var storeThemes by mutableStateOf<List<MbStoreTheme>>(emptyList())
        private set
    var downloadedThemeIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var currentTargetUuid by mutableStateOf(preferences.target(targetPackage)?.first ?: DEFAULT_UUID)
        private set
    var currentTargetName by mutableStateOf(preferences.target(targetPackage)?.second ?: DEFAULT_NAME)
        private set
    var selectedTheme by mutableStateOf<CommunityTheme?>(null)
    var isTargetPickerOpen by mutableStateOf(false)
    var isFlashScreenOpen by mutableStateOf(false)
    var activeInstallConfig by mutableStateOf<InjectConfig?>(null)
        private set
    var activeTargetName by mutableStateOf("")
        private set
    var flashingStatus by mutableStateOf(FlashingStatus.FLASHING)
        private set
    var flashFailedReason by mutableStateOf<String?>(null)
        private set
    var isBusy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    val injectLogs = mutableStateListOf<String>()

    fun refresh(forceRefresh: Boolean = false): Job {
        if (isBusy) return viewModelScope.launch { }
        refreshJob?.cancel()
        val repository = rootRepository
        return viewModelScope.launch {
            try {
                val root = repository.isRootAvailable()
                val installed = repository.isMbInstalled()
                ensureActive()
                isRootGranted = root
                isMbInstalled = installed
                if (!root || !installed) installedThemes = emptyList()
                val store = themeRepository.getStoreThemes()
                val community = themeRepository.getCommunityThemes(forceRefresh)
                val downloaded = withContext(Dispatchers.IO) {
                    community.filter { themeRepository.isThemeDownloaded(it) }.map { it.id }.toSet()
                }
                val targets = if (root && installed) repository.getInstalledThemes(store) else emptyList()
                ensureActive()
                if (repository !== rootRepository) return@launch
                storeThemes = store
                communityThemes = community
                downloadedThemeIds = downloaded
                installedThemes = targets
                if (preferences.target(targetPackage) == null && selectedTheme == null) {
                    targets.firstOrNull()?.let {
                        selectTarget(it.uuid, it.storeTheme?.displayName ?: it.uuid)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                installedThemes = emptyList()
                message = "Không thể làm mới dữ liệu: ${e.message}"
            }
        }.also { refreshJob = it }
    }

    fun changePackage(value: String) {
        if (isBusy) { message = "Vui lòng đợi tác vụ hiện tại hoàn tất"; return }
        val pkg = value.trim()
        if (pkg == targetPackage) return
        try {
            val nextRepository = rootFactory(context, pkg)
            preferences.savePackage(pkg)
            refreshJob?.cancel()
            targetPackage = pkg
            rootRepository = nextRepository
            installedThemes = emptyList()
            isRootGranted = false
            isMbInstalled = false
            selectedTheme = null
            isTargetPickerOpen = false
            val target = preferences.target(pkg)
            currentTargetUuid = target?.first ?: DEFAULT_UUID
            currentTargetName = target?.second ?: DEFAULT_NAME
            // A remembered installation for another app is not evidence for this package.
            ThemeConfig.clearAppliedTheme(context)
            refresh()
        } catch (e: Exception) { message = "Gói ứng dụng không hợp lệ: ${e.message}" }
    }

    fun selectTheme(theme: CommunityTheme) {
        if (isBusy) return
        selectedTheme = theme
        if (preferences.target(targetPackage) == null) {
            storeThemes.find { it.uuid.equals(theme.defaultTargetUuid, true) }?.let {
                currentTargetUuid = it.uuid
                currentTargetName = it.displayName
            }
        }
    }

    fun selectTarget(uuid: String, name: String) {
        preferences.saveTarget(targetPackage, uuid, name)
        currentTargetUuid = uuid
        currentTargetName = name
    }

    fun deleteDownloaded(theme: CommunityTheme): Job = viewModelScope.launch {
        if (isBusy) return@launch
        isBusy = true
        try {
            refreshJob?.cancelAndJoin()
            withContext(Dispatchers.IO) { themeRepository.deleteDownloadedTheme(theme) }.getOrThrow()
            downloadedThemeIds = downloadedThemeIds - theme.id
            if (theme.isCustomImport) communityThemes = communityThemes.filterNot { it.id == theme.id }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { message = "Không thể xoá bản tải: ${e.message}"
        } finally { isBusy = false }
    }

    fun importZip(uri: Uri, name: String): Job = viewModelScope.launch {
        if (isBusy) return@launch
        isBusy = true
        try {
            refreshJob?.cancelAndJoin()
            val imported = themeRepository.importCustomZip(uri, name).getOrThrow()
            communityThemes = communityThemes + imported
            downloadedThemeIds = downloadedThemeIds + imported.id
            selectedTheme = imported
            message = "Nạp theme thành công!"
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { message = "Lỗi nạp ZIP: ${e.message}"
        } finally { isBusy = false }
    }

    /** Reset runs in the ViewModel scope even if the confirmation UI is recreated. */
    suspend fun resetThemes(): Result<Unit> = viewModelScope.async {
        if (isBusy) return@async Result.failure(IllegalStateException("Một tác vụ khác đang chạy"))
        isBusy = true
        try {
            refreshJob?.cancelAndJoin()
            val result = rootRepository.resetAllThemes()
            if (result.isSuccess) {
                ThemeConfig.clearAppliedTheme(context)
                installedThemes = emptyList()
            }
            result
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e)
        } finally { isBusy = false }
    }.await()

    fun startInject(config: InjectConfig): Job = viewModelScope.launch {
        if (isBusy) return@launch
        isBusy = true
        val repository = rootRepository
        selectedTheme = null
        isTargetPickerOpen = false
        activeInstallConfig = config
        activeTargetName = currentTargetName
        flashingStatus = FlashingStatus.FLASHING
        flashFailedReason = null
        injectLogs.clear()
        injectLogs.addAll(FlashScreenConstants.createInitialLogs(config, activeTargetName, repository.targetPackage))
        isFlashScreenOpen = true
        try {
            refreshJob?.cancelAndJoin()
            check(repository.isRootAvailable()) { "Ứng dụng chưa được cấp quyền Root" }
            check(repository.isMbInstalled()) { "Chưa cài đặt ứng dụng mục tiêu" }
            val local = withContext(Dispatchers.IO) { themeRepository.isThemeDownloaded(config.sourceTheme) }
            val themeDir = if (local) themeRepository.getThemeDir(config.sourceTheme.id) else {
                injectLogs.add("- Đang tải theme từ máy chủ...")
                var lastPercent = -1
                themeRepository.downloadTheme(config.sourceTheme) { progress ->
                    val percent = (progress * 100).toInt().coerceIn(0, 100)
                    if (percent / 25 > lastPercent / 25 || lastPercent < 0) {
                        lastPercent = percent
                        viewModelScope.launch(Dispatchers.Main.immediate) { injectLogs.add("- Tiến độ tải: $percent%") }
                    }
                }.getOrThrow()
            }
            val result = repository.injectTheme(config, themeDir) { line ->
                viewModelScope.launch(Dispatchers.Main.immediate) { injectLogs.add(line) }
            }
            check(result.isSuccess) { result.errorMessage ?: "Nạp theme thất bại" }
            ThemeConfig.saveAppliedTheme(context, config.sourceTheme.name, activeTargetName,
                config.usePriorityVariant, config.sourceTheme.id)
            flashingStatus = FlashingStatus.SUCCESS
            if (config.autoLaunchDeeplink) {
                repository.launchMbBank(true, config.targetUuid).exceptionOrNull()?.let {
                    injectLogs.add("[Cảnh báo] Theme đã nạp, nhưng không thể mở MB Bank: ${it.message}")
                    message = "Theme đã nạp. Hãy mở MB Bank thủ công."
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            flashFailedReason = e.message ?: "Có lỗi xảy ra trong quá trình cài đặt"
            injectLogs.add("[Lỗi] $flashFailedReason")
            flashingStatus = FlashingStatus.FAILED
        } finally {
            isBusy = false
        }
        refresh()
    }

    companion object {
        const val DEFAULT_UUID = "aa3cb89a-8325-41b4-b59b-dfaea086cf80"
        const val DEFAULT_NAME = "Cánh Én Mùa Xuân"
    }
}

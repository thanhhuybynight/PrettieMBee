package anhiutangerine.prettiembee.data.repository

import android.content.Context
import anhiutangerine.prettiembee.data.model.InjectConfig
import anhiutangerine.prettiembee.data.model.InjectResult
import anhiutangerine.prettiembee.data.model.InstalledTheme
import anhiutangerine.prettiembee.data.model.MbStoreTheme
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RootRepository(private val context: Context) {

    var targetPackage: String = "com.mbmobile"
        set(value) {
            field = value
            cachedDataDir = null
        }

    private var cachedDataDir: String? = null

    suspend fun resolveDataDir(): String = withContext(Dispatchers.IO) {
        cachedDataDir?.let { return@withContext it }

        // Candidate paths for app private data dir in Android
        val candidates = listOf(
            "/data/data/$targetPackage",
            "/data/user/0/$targetPackage",
            "/data/user_de/0/$targetPackage"
        )

        for (candidate in candidates) {
            val res = Shell.cmd("test -d '$candidate' && echo 1 || echo 0").exec()
            if (res.out.firstOrNull()?.trim() == "1") {
                cachedDataDir = candidate
                return@withContext candidate
            }
        }

        // Query PackageManager via dumpsys
        try {
            val dumpRes = Shell.cmd("dumpsys package $targetPackage 2>/dev/null | grep -m 1 'dataDir='").exec()
            val line = dumpRes.out.firstOrNull()
            if (!line.isNullOrBlank() && line.contains("dataDir=")) {
                val extracted = line.substringAfter("dataDir=").trim().substringBefore(" ")
                if (extracted.isNotBlank()) {
                    cachedDataDir = extracted
                    return@withContext extracted
                }
            }
        } catch (_: Exception) {}

        val defaultPath = "/data/data/$targetPackage"
        cachedDataDir = defaultPath
        defaultPath
    }

    val mbThemeBase: String
        get() = "${cachedDataDir ?: "/data/data/$targetPackage"}/app_flutter/app_theme/unzip"

    suspend fun getMbThemeBase(): String {
        return "${resolveDataDir()}/app_flutter/app_theme/unzip"
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isMbInstalled(): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = Shell.cmd("pm path $targetPackage").exec()
            res.isSuccess && res.out.any { it.contains("package:") }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getInstalledThemes(storeCatalog: List<MbStoreTheme>): List<InstalledTheme> = withContext(Dispatchers.IO) {
        try {
            val themeBase = getMbThemeBase()
            val resultList = mutableListOf<InstalledTheme>()
            val listRes = Shell.cmd("ls -1 '$themeBase' 2>/dev/null").exec()
            if (!listRes.isSuccess || listRes.out.isEmpty()) return@withContext emptyList()

            for (uuid in listRes.out) {
                val cleanUuid = uuid.trim()
                if (cleanUuid.isBlank() || !cleanUuid.contains("-")) continue

                val countRes = Shell.cmd("ls -1 '$themeBase/$cleanUuid/images/'*.png 2>/dev/null | wc -l").exec()
                val imgCount = countRes.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0

                val tokenRes = Shell.cmd("test -f '$themeBase/$cleanUuid/theme/token.json' && echo 1 || echo 0").exec()
                val hasToken = tokenRes.out.firstOrNull()?.trim() == "1"

                val matchedStoreTheme = storeCatalog.find { it.uuid.equals(cleanUuid, ignoreCase = true) }

                resultList.add(
                    InstalledTheme(
                        uuid = cleanUuid,
                        storeTheme = matchedStoreTheme,
                        imageCount = imgCount,
                        hasTokenJson = hasToken
                    )
                )
            }
            resultList
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun backupTheme(targetUuid: String): Boolean = withContext(Dispatchers.IO) {
        val themeBase = getMbThemeBase()
        val backupDir = File(context.filesDir, "backups/$targetUuid")
        backupDir.mkdirs()
        val res = Shell.cmd(
            "mkdir -p '${backupDir.absolutePath}'",
            "cp -rf '$themeBase/$targetUuid/.' '${backupDir.absolutePath}/'"
        ).exec()
        res.isSuccess
    }

    suspend fun restoreTheme(targetUuid: String): Boolean = withContext(Dispatchers.IO) {
        val themeBase = getMbThemeBase()
        val backupDir = File(context.filesDir, "backups/$targetUuid")
        if (!backupDir.exists()) return@withContext false

        val res = Shell.cmd(
            "am force-stop $targetPackage",
            "cp -rf '${backupDir.absolutePath}/.' '$themeBase/$targetUuid/'",
            "restorecon -R '$themeBase/$targetUuid'"
        ).exec()
        res.isSuccess
    }

    suspend fun injectTheme(
        config: InjectConfig,
        themeDir: File,
        onProgress: (String) -> Unit
    ): InjectResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
            onProgress(msg)
        }

        try {
            log("[Chuẩn bị] Bắt đầu nạp theme: ${config.sourceTheme.name}")
            log("[Thông tin] Ứng dụng mục tiêu: $targetPackage")

            // 1. Force stop MB Bank
            log("[Tiến trình] Dừng ứng dụng $targetPackage...")
            Shell.cmd("am force-stop $targetPackage").exec()

            // 2. Resolve Data Directory
            val dataDir = resolveDataDir()
            log("[Thông tin] Thư mục dữ liệu: $dataDir")

            // 3. Query UID/GID with multi-tier fallback
            log("[Tiến trình] Kiểm tra định danh ứng dụng (UID/GID)...")
            var uidGid: String? = null

            // Strategy 1: stat -c '%u:%g' on dataDir
            val uidRes = Shell.cmd("stat -c '%u:%g' '$dataDir' 2>/dev/null").exec()
            val statOut = uidRes.out.firstOrNull()?.trim()
            if (!statOut.isNullOrBlank() && statOut.contains(":") && !statOut.startsWith("0:0")) {
                uidGid = statOut
            }

            // Strategy 2: pm list packages -U
            if (uidGid == null) {
                val pmRes = Shell.cmd("pm list packages -U $targetPackage 2>/dev/null").exec()
                val match = pmRes.out.firstOrNull { it.contains("uid:") }
                val uid = match?.substringAfter("uid:")?.trim()?.takeWhile { it.isDigit() }
                if (!uid.isNullOrBlank()) {
                    uidGid = "$uid:$uid"
                }
            }

            // Strategy 3: dumpsys package userId=
            if (uidGid == null) {
                val dumpRes = Shell.cmd("dumpsys package $targetPackage 2>/dev/null | grep -m 1 'userId='").exec()
                val userId = dumpRes.out.firstOrNull()?.substringAfter("userId=")?.trim()?.takeWhile { it.isDigit() }
                if (!userId.isNullOrBlank()) {
                    uidGid = "$userId:$userId"
                }
            }

            // Strategy 4: stat on APK path
            if (uidGid == null) {
                val pathRes = Shell.cmd("pm path $targetPackage 2>/dev/null").exec()
                val apkPath = pathRes.out.firstOrNull { it.startsWith("package:") }?.substringAfter("package:")?.trim()
                if (!apkPath.isNullOrBlank()) {
                    val apkStat = Shell.cmd("stat -c '%u:%g' '$apkPath' 2>/dev/null").exec()
                    val resUid = apkStat.out.firstOrNull()?.trim()
                    if (!resUid.isNullOrBlank() && resUid.contains(":")) {
                        uidGid = resUid
                    }
                }
            }

            if (uidGid.isNullOrBlank() || !uidGid.contains(":")) {
                val err = "Không thể lấy UID/GID của $targetPackage. Hãy đảm bảo MB Bank đã được cài đặt và mở ít nhất một lần!"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }
            log("[Thông tin] UID/GID ứng dụng: $uidGid")

            // 4. Verify source theme assets on disk
            val sourceImages = File(themeDir, "images")
            val sourceThemeFolder = File(themeDir, "theme")
            val tokenFile = if (config.usePriorityVariant && File(sourceThemeFolder, "token_priority.json").exists()) {
                log("[Tiến trình] Áp dụng cấu hình Priority Tokens...")
                File(sourceThemeFolder, "token_priority.json")
            } else {
                File(sourceThemeFolder, "token.json")
            }

            if (!sourceImages.exists() || !tokenFile.exists()) {
                val err = "Thiếu tài nguyên theme tại ${themeDir.absolutePath}"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }

            // Ensure source theme directory is accessible by root
            Shell.cmd("chmod -R 755 '${themeDir.absolutePath}' 2>/dev/null").exec()

            // 5. Target folder
            val themeBase = "$dataDir/app_flutter/app_theme/unzip"
            val targetDir = "$themeBase/${config.targetUuid}"
            log("[Tiến trình] Chuẩn bị thư mục đích: $targetDir")
            val prepCmd = Shell.cmd(
                "mkdir -p '$targetDir'",
                "rm -rf '$targetDir/images' '$targetDir/theme'"
            ).exec()

            if (!prepCmd.isSuccess) {
                val prepErr = (prepCmd.err + prepCmd.out).filter { it.isNotBlank() }.joinToString("\n")
                val err = "Không thể khởi tạo thư mục đích:\n$prepErr"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 6. Copy from source to target using root
            log("[Tiến trình] Ghi đè tài nguyên vào thư mục theme của MB Bank...")
            val copyCmd = Shell.cmd(
                "cp -rf '${sourceImages.absolutePath}' '$targetDir/'",
                "mkdir -p '$targetDir/theme'",
                "cp -f '${tokenFile.absolutePath}' '$targetDir/theme/token.json'"
            ).exec()

            if (!copyCmd.isSuccess) {
                val errorDetails = (copyCmd.err + copyCmd.out).filter { it.isNotBlank() }.joinToString("\n")
                val err = "Lỗi sao chép tập tin (mã lỗi ${copyCmd.code}):\n${errorDetails.ifBlank { "Lệnh sao chép thất bại nhưng không có thông điệp lỗi chi tiết từ shell." }}"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 7. Fix permissions and ownership recursively for Flutter themes
            log("[Tiến trình] Phân quyền hạn và chủ sở hữu ($uidGid)...")
            val flutterDir = "$dataDir/app_flutter"
            Shell.cmd(
                "chown -R $uidGid '$flutterDir'",
                "chmod 755 '$dataDir/app_flutter' 2>/dev/null",
                "chmod 755 '$dataDir/app_flutter/app_theme' 2>/dev/null",
                "chmod 755 '$dataDir/app_flutter/app_theme/unzip' 2>/dev/null",
                "chmod -R 755 '$targetDir'",
                "chmod -R a+r '$targetDir'"
            ).exec()

            // 8. Restore SELinux context
            log("[Tiến trình] Phục hồi ngữ cảnh SELinux (restorecon)...")
            Shell.cmd("restorecon -R '$flutterDir'").exec()

            log("[Thành công] Nạp theme hoàn tất.")

            // 9. Launch post-actions
            if (config.autoLaunchDeeplink) {
                log("[Hành động] Mở trang chi tiết theme trong MB Bank...")
                val deeplink = "mbbank://installingnew?af_force_deeplink=true&ad_dp=theme_detail&id=${config.targetUuid}"
                Shell.cmd("am start -a android.intent.action.VIEW -d '$deeplink' $targetPackage").exec()
            } else if (config.autoLaunchMb) {
                log("[Hành động] Khởi chạy lại ứng dụng MB Bank...")
                Shell.cmd("am start -n $targetPackage/io.flutter.plugins.MainActivity").exec()
            }

            return@withContext InjectResult(true, logs)
        } catch (e: Exception) {
            val err = "Ngoại lệ: ${e.message}"
            log("[Lỗi] $err")
            return@withContext InjectResult(false, logs, err)
        }
    }
}

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

    val mbThemeBase: String
        get() = "/data/data/$targetPackage/app_flutter/app_theme/unzip"

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
            val resultList = mutableListOf<InstalledTheme>()
            val listRes = Shell.cmd("ls -1 $mbThemeBase 2>/dev/null").exec()
            if (!listRes.isSuccess || listRes.out.isEmpty()) return@withContext emptyList()

            for (uuid in listRes.out) {
                val cleanUuid = uuid.trim()
                if (cleanUuid.isBlank() || !cleanUuid.contains("-")) continue

                val countRes = Shell.cmd("ls -1 $mbThemeBase/$cleanUuid/images/*.png 2>/dev/null | wc -l").exec()
                val imgCount = countRes.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0

                val tokenRes = Shell.cmd("test -f $mbThemeBase/$cleanUuid/theme/token.json && echo 1 || echo 0").exec()
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
        val backupDir = File(context.filesDir, "backups/$targetUuid")
        backupDir.mkdirs()
        val res = Shell.cmd(
            "mkdir -p '${backupDir.absolutePath}'",
            "cp -rf '$mbThemeBase/$targetUuid/'* '${backupDir.absolutePath}/'"
        ).exec()
        res.isSuccess
    }

    suspend fun restoreTheme(targetUuid: String): Boolean = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups/$targetUuid")
        if (!backupDir.exists()) return@withContext false

        val res = Shell.cmd(
            "am force-stop $targetPackage",
            "cp -rf '${backupDir.absolutePath}/'* '$mbThemeBase/$targetUuid/'",
            "restorecon -R $mbThemeBase/$targetUuid"
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
            log("🐝 Bắt đầu nạp theme ${config.sourceTheme.name}...")
            log("🎯 Gói mục tiêu: $targetPackage")

            // 1. Force stop MB Bank
            log("🛑 Buộc dừng ứng dụng $targetPackage...")
            Shell.cmd("am force-stop $targetPackage").exec()

            // 2. Query UID/GID
            log("🔍 Kiểm tra phân quyền của $targetPackage...")
            val uidRes = Shell.cmd("stat -c '%u:%g' /data/data/$targetPackage").exec()
            val uidGid = uidRes.out.firstOrNull()?.trim()
            if (uidGid.isNullOrBlank() || !uidGid.contains(":")) {
                val err = "Không thể lấy UID/GID của $targetPackage. Ứng dụng đã được cài đặt chưa?"
                log("❌ $err")
                return@withContext InjectResult(false, logs, err)
            }
            log("✅ UID/GID của ứng dụng: $uidGid")

            // 3. Verify source theme assets on disk
            val sourceImages = File(themeDir, "images")
            val sourceThemeFolder = File(themeDir, "theme")
            val tokenFile = if (config.usePriorityVariant && File(sourceThemeFolder, "token_priority.json").exists()) {
                log("✨ Kích hoạt biến thể Priority Tokens...")
                File(sourceThemeFolder, "token_priority.json")
            } else {
                File(sourceThemeFolder, "token.json")
            }

            if (!sourceImages.exists() || !tokenFile.exists()) {
                val err = "Thiếu asset tại ${themeDir.absolutePath}"
                log("❌ $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 4. Target folder
            val targetDir = "$mbThemeBase/${config.targetUuid}"
            log("📁 Chuẩn bị thư mục đích: $targetDir")
            Shell.cmd(
                "mkdir -p '$targetDir/images'",
                "mkdir -p '$targetDir/theme'"
            ).exec()

            // 5. Copy from source to target using root
            log("🚀 Đang ghi đè asset vào thư mục theme của MB Bank...")
            val copyCmd = Shell.cmd(
                "cp -rf '${sourceImages.absolutePath}/'* '$targetDir/images/'",
                "cp -f '${tokenFile.absolutePath}' '$targetDir/theme/token.json'"
            ).exec()

            if (!copyCmd.isSuccess) {
                val err = "Lỗi khi sao chép file: ${copyCmd.err.joinToString("\n")}"
                log("❌ $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 6. Fix permissions and ownership
            log("🔒 Thiết lập quyền hạn và chủ sở hữu ($uidGid)...")
            Shell.cmd(
                "chown -R $uidGid '$targetDir'",
                "chmod -R 755 '$targetDir'",
                "chmod 644 '$targetDir/images/'* 2>/dev/null",
                "chmod 644 '$targetDir/theme/'* 2>/dev/null"
            ).exec()

            // 7. Restore SELinux context
            log("🛡️ Phục hồi ngữ cảnh SELinux (restorecon)...")
            Shell.cmd("restorecon -R '$targetDir'").exec()

            log("🎉 Nạp theme thành công 100%!")

            // 8. Launch post-actions
            if (config.autoLaunchDeeplink) {
                log("🔗 Mở trang chi tiết Theme trong MB Bank...")
                val deeplink = "mbbank://installingnew?af_force_deeplink=true&ad_dp=theme_detail&id=${config.targetUuid}"
                Shell.cmd("am start -a android.intent.action.VIEW -d '$deeplink' $targetPackage").exec()
            } else if (config.autoLaunchMb) {
                log("🚀 Khởi chạy lại MB Bank...")
                Shell.cmd("am start -n $targetPackage/io.flutter.plugins.MainActivity").exec()
            }

            return@withContext InjectResult(true, logs)
        } catch (e: Exception) {
            val err = "Lỗi ngoại lệ: ${e.message}"
            log("❌ $err")
            return@withContext InjectResult(false, logs, err)
        }
    }
}

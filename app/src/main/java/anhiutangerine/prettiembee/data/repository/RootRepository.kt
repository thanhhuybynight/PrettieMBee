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
import java.io.FileOutputStream

class RootRepository(private val context: Context) {

    companion object {
        const val MB_PACKAGE = "com.mbmobile"
        const val MB_THEME_BASE = "/data/data/com.mbmobile/app_flutter/app_theme/unzip"
        const val STAGING_DIR = "/data/local/tmp/prettiembee_staging"
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isMbInstalled(): Boolean = withContext(Dispatchers.IO) {
        val res = Shell.cmd("pm path $MB_PACKAGE").exec()
        res.isSuccess && res.out.any { it.contains("package:") }
    }

    suspend fun getInstalledThemes(storeCatalog: List<MbStoreTheme>): List<InstalledTheme> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<InstalledTheme>()
        val listRes = Shell.cmd("ls -1 $MB_THEME_BASE 2>/dev/null").exec()
        if (!listRes.isSuccess || listRes.out.isEmpty()) return@withContext emptyList()

        for (uuid in listRes.out) {
            val cleanUuid = uuid.trim()
            if (cleanUuid.isBlank() || !cleanUuid.contains("-")) continue

            val countRes = Shell.cmd("ls -1 $MB_THEME_BASE/$cleanUuid/images/*.png 2>/dev/null | wc -l").exec()
            val imgCount = countRes.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0

            val tokenRes = Shell.cmd("test -f $MB_THEME_BASE/$cleanUuid/theme/token.json && echo 1 || echo 0").exec()
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
    }

    suspend fun backupTheme(targetUuid: String): Boolean = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups/$targetUuid")
        backupDir.mkdirs()
        val res = Shell.cmd(
            "mkdir -p '${backupDir.absolutePath}'",
            "cp -rf '$MB_THEME_BASE/$targetUuid/'* '${backupDir.absolutePath}/'"
        ).exec()
        res.isSuccess
    }

    suspend fun restoreTheme(targetUuid: String): Boolean = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups/$targetUuid")
        if (!backupDir.exists()) return@withContext false

        val res = Shell.cmd(
            "am force-stop $MB_PACKAGE",
            "cp -rf '${backupDir.absolutePath}/'* '$MB_THEME_BASE/$targetUuid/'",
            "restorecon -R $MB_THEME_BASE/$targetUuid"
        ).exec()
        res.isSuccess
    }

    suspend fun injectTheme(
        config: InjectConfig,
        onProgress: (String) -> Unit
    ): InjectResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
            onProgress(msg)
        }

        try {
            log("🐝 Bắt đầu nạp theme ${config.sourceTheme.name}...")

            // 1. Force stop MB Bank
            log("🛑 Buộc dừng ứng dụng MB Bank...")
            Shell.cmd("am force-stop $MB_PACKAGE").exec()

            // 2. Query UID/GID
            log("🔍 Kiểm tra phân quyền của MB Bank...")
            val uidRes = Shell.cmd("stat -c '%u:%g' /data/data/$MB_PACKAGE").exec()
            val uidGid = uidRes.out.firstOrNull()?.trim()
            if (uidGid.isNullOrBlank() || !uidGid.contains(":")) {
                val err = "Không thể lấy UID/GID của $MB_PACKAGE. MB Bank đã được cài đặt chưa?"
                log("❌ $err")
                return@withContext InjectResult(false, logs, err)
            }
            log("✅ UID/GID của MB Bank: $uidGid")

            // 3. Extract assets to internal cache staging
            log("📦 Chuẩn bị asset của theme...")
            val stagingDir = File(context.cacheDir, "staging_${System.currentTimeMillis()}")
            stagingDir.mkdirs()

            val assetManager = context.assets
            val themeAssetPath = config.sourceTheme.assetDir // e.g. "themes/furina"

            // Copy images from assets
            val stagingImages = File(stagingDir, "images").apply { mkdirs() }
            val imageList = assetManager.list("$themeAssetPath/images") ?: emptyArray()
            log("🖼️ Đang trích xuất ${imageList.size} hình ảnh...")
            for (img in imageList) {
                assetManager.open("$themeAssetPath/images/$img").use { input ->
                    FileOutputStream(File(stagingImages, img)).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Copy token.json
            val stagingTheme = File(stagingDir, "theme").apply { mkdirs() }
            val tokenName = if (config.usePriorityVariant && config.sourceTheme.supportsPriority) {
                log("✨ Kích hoạt biến thể Priority Design Tokens...")
                "token_priority.json"
            } else {
                "token.json"
            }

            val tokenSourcePath = "$themeAssetPath/theme/$tokenName"
            val fallbackTokenPath = "$themeAssetPath/theme/token.json"

            val tokenInput = try {
                assetManager.open(tokenSourcePath)
            } catch (e: Exception) {
                assetManager.open(fallbackTokenPath)
            }

            tokenInput.use { input ->
                FileOutputStream(File(stagingTheme, "token.json")).use { output ->
                    input.copyTo(output)
                }
            }

            // 4. Target folder
            val targetDir = "$MB_THEME_BASE/${config.targetUuid}"
            log("📁 Chuẩn bị thư mục đích: $targetDir")
            Shell.cmd(
                "mkdir -p '$targetDir/images'",
                "mkdir -p '$targetDir/theme'"
            ).exec()

            // 5. Copy from staging to target using root
            log("🚀 Đang ghi đè asset vào thư mục theme của MB Bank...")
            val copyCmd = Shell.cmd(
                "cp -rf '${stagingImages.absolutePath}/'* '$targetDir/images/'",
                "cp -rf '${stagingTheme.absolutePath}/'* '$targetDir/theme/'"
            ).exec()

            if (!copyCmd.isSuccess) {
                val err = "Lỗi khi sao chép file vào thư mục appdata: ${copyCmd.err.joinToString("\n")}"
                log("❌ $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 6. Fix permissions and ownership
            log("🔒 Thiết lập quyền hạn và chủ sở hữu ($uidGid)...")
            val permCmd = Shell.cmd(
                "chown -R $uidGid '$targetDir'",
                "chmod -R 755 '$targetDir'",
                "chmod 644 '$targetDir/images/'* 2>/dev/null",
                "chmod 644 '$targetDir/theme/'* 2>/dev/null"
            ).exec()

            // 7. Restore SELinux context
            log("🛡️ Phục hồi ngữ cảnh SELinux (restorecon)...")
            Shell.cmd("restorecon -R '$targetDir'").exec()

            // Clean staging
            stagingDir.deleteRecursively()

            log("🎉 Nạp theme thành công 100%!")

            // 8. Launch post-actions
            if (config.autoLaunchDeeplink) {
                log("🔗 Mở trang chi tiết Theme trong MB Bank...")
                val deeplink = "mbbank://installingnew?af_force_deeplink=true&ad_dp=theme_detail&id=${config.targetUuid}"
                Shell.cmd("am start -a android.intent.action.VIEW -d '$deeplink' $MB_PACKAGE").exec()
            } else if (config.autoLaunchMb) {
                log("🚀 Khởi chạy lại MB Bank...")
                Shell.cmd("am start -n $MB_PACKAGE/io.flutter.plugins.MainActivity").exec()
            }

            return@withContext InjectResult(true, logs)
        } catch (e: Exception) {
            val err = "Lỗi ngoại lệ: ${e.message}"
            log("❌ $err")
            return@withContext InjectResult(false, logs, err)
        }
    }
}

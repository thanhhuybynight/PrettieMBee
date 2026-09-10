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
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

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

    suspend fun resolveAppPath(file: File): String = withContext(Dispatchers.IO) {
        val candidates = listOf(
            file.absolutePath,
            file.canonicalPath,
            file.absolutePath.replace("/data/user/0/", "/data/data/"),
            file.absolutePath.replace("/data/data/", "/data/user/0/")
        ).distinct()

        for (candidate in candidates) {
            val res = Shell.cmd("test -e '$candidate' && echo 1 || echo 0").exec()
            if (res.out.firstOrNull()?.trim() == "1") {
                return@withContext candidate
            }
        }
        file.absolutePath
    }

    private fun writeDirectoryToTarStream(sourceDir: File, outputStream: OutputStream) {
        val files = sourceDir.listFiles() ?: return
        val buffer = ByteArray(8192)
        for (file in files) {
            if (!file.isFile) continue
            val name = file.name
            val length = file.length()
            val mtime = file.lastModified() / 1000

            val header = ByteArray(512)
            val nameBytes = name.toByteArray(StandardCharsets.US_ASCII)
            System.arraycopy(nameBytes, 0, header, 0, minOf(nameBytes.size, 100))

            writeOctal(header, 100, 8, 420L) // 0644 octal
            writeOctal(header, 108, 8, 0L)
            writeOctal(header, 116, 8, 0L)
            writeOctal(header, 124, 12, length)
            writeOctal(header, 136, 12, mtime)

            for (i in 148 until 156) header[i] = ' '.code.toByte()
            header[156] = '0'.code.toByte()

            header[257] = 'u'.code.toByte()
            header[258] = 's'.code.toByte()
            header[259] = 't'.code.toByte()
            header[260] = 'a'.code.toByte()
            header[261] = 'r'.code.toByte()
            header[262] = 0
            header[263] = '0'.code.toByte()
            header[264] = '0'.code.toByte()

            var sum = 0L
            for (b in header) {
                sum += (b.toInt() and 0xFF)
            }
            val chkStr = String.format(Locale.US, "%06o", sum)
            val chkBytes = chkStr.toByteArray(StandardCharsets.US_ASCII)
            System.arraycopy(chkBytes, 0, header, 148, 6)
            header[154] = 0
            header[155] = ' '.code.toByte()

            outputStream.write(header)

            file.inputStream().use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
            }

            val remainder = (length % 512).toInt()
            if (remainder > 0) {
                outputStream.write(ByteArray(512 - remainder))
            }
        }
        outputStream.write(ByteArray(1024))
        outputStream.flush()
    }

    private fun writeOctal(buf: ByteArray, offset: Int, length: Int, value: Long) {
        val s = java.lang.Long.toOctalString(value)
        var idx = offset + length - 1
        buf[idx--] = 0
        var sIdx = s.length - 1
        while (idx >= offset && sIdx >= 0) {
            buf[idx--] = s[sIdx--].code.toByte()
        }
        while (idx >= offset) {
            buf[idx--] = '0'.code.toByte()
        }
    }

    private suspend fun streamTarToTarget(
        sourceDir: File,
        targetDir: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val suCommands = listOf(
            arrayOf("su", "-mm", "-c", "toybox tar -xf - -C '$targetDir' || tar -xf - -C '$targetDir'"),
            arrayOf("su", "-c", "toybox tar -xf - -C '$targetDir' || tar -xf - -C '$targetDir'")
        )

        var lastError: Exception? = null
        for (cmd in suCommands) {
            try {
                val proc = ProcessBuilder(*cmd).start()
                proc.outputStream.use { os ->
                    writeDirectoryToTarStream(sourceDir, os)
                }
                val exitCode = proc.waitFor()
                if (exitCode == 0) {
                    return@withContext Result.success(Unit)
                } else {
                    val err = proc.errorStream.bufferedReader().use { it.readText() }
                    lastError = Exception("tar kết thúc với mã $exitCode: $err")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception("Không thể thực thi lệnh tar qua su"))
    }

    private suspend fun streamFileToTarget(
        sourceFile: File,
        targetPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val suCommands = listOf(
            arrayOf("su", "-mm", "-c", "cat > '$targetPath'"),
            arrayOf("su", "-c", "cat > '$targetPath'")
        )

        var lastError: Exception? = null
        for (cmd in suCommands) {
            try {
                val proc = ProcessBuilder(*cmd).start()
                sourceFile.inputStream().use { input ->
                    proc.outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                val exitCode = proc.waitFor()
                if (exitCode == 0) {
                    return@withContext Result.success(Unit)
                } else {
                    val err = proc.errorStream.bufferedReader().use { it.readText() }
                    lastError = Exception("cat kết thúc với mã $exitCode: $err")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception("Không thể ghi file qua su"))
    }

    suspend fun backupTheme(targetUuid: String): Boolean = withContext(Dispatchers.IO) {
        val themeBase = getMbThemeBase()
        val backupDir = File(context.filesDir, "backups/$targetUuid")
        backupDir.mkdirs()
        val resolvedBackupDir = resolveAppPath(backupDir)
        val res = Shell.cmd(
            "mkdir -p '$resolvedBackupDir'",
            "cp -rf '$themeBase/$targetUuid/.' '$resolvedBackupDir/'"
        ).exec()
        res.isSuccess
    }

    suspend fun restoreTheme(targetUuid: String): Boolean = withContext(Dispatchers.IO) {
        val themeBase = getMbThemeBase()
        val backupDir = File(context.filesDir, "backups/$targetUuid")
        if (!backupDir.exists()) return@withContext false
        val resolvedBackupDir = resolveAppPath(backupDir)

        val res = Shell.cmd(
            "am force-stop $targetPackage",
            "cp -rf '$resolvedBackupDir/.' '$themeBase/$targetUuid/'",
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
            val myPkg = context.packageName
            Shell.cmd(
                "chmod 755 '/data/data/$myPkg' '/data/data/$myPkg/files' 2>/dev/null || true",
                "chmod 755 '/data/user/0/$myPkg' '/data/user/0/$myPkg/files' 2>/dev/null || true",
                "chmod -R 755 '${themeDir.absolutePath}' 2>/dev/null || true"
            ).exec()

            // 5. Target folder
            val themeBase = "$dataDir/app_flutter/app_theme/unzip"
            val targetDir = "$themeBase/${config.targetUuid}"
            log("[Tiến trình] Chuẩn bị thư mục đích: $targetDir")
            val prepScript = """
                exec 2>&1
                mkdir -p '$targetDir'
                rm -rf '$targetDir/images' '$targetDir/theme'
                mkdir -p '$targetDir/images'
                mkdir -p '$targetDir/theme'
            """.trimIndent()
            val prepCmd = Shell.cmd(prepScript).exec()
            val prepOutput = (prepCmd.out + prepCmd.err).filter { it.isNotBlank() }.joinToString("\n")
            if (prepOutput.isNotBlank()) {
                log("[Shell Prep] $prepOutput")
            }

            // Verify target directory exists
            val testTarget = Shell.cmd("test -d '$targetDir' && echo 1 || echo 0").exec()
            val targetExists = testTarget.out.firstOrNull()?.trim() == "1"

            if (!targetExists) {
                val err = "Không thể khởi tạo thư mục đích:\n${prepOutput.ifBlank { "Lệnh mkdir không thể tạo thư mục tại $targetDir" }}"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 6. Copy from source to target using root with fallback stream
            log("[Tiến trình] Ghi đè tài nguyên vào thư mục theme của MB Bank...")
            val resolvedImages = resolveAppPath(sourceImages)
            val resolvedToken = resolveAppPath(tokenFile)
            log("[Thông tin] Nguồn ảnh: $resolvedImages")
            log("[Thông tin] Nguồn token: $resolvedToken")

            var copySucceeded = false
            val copyScript = """
                exec 2>&1
                mkdir -p '$targetDir/images'
                mkdir -p '$targetDir/theme'
                if ! cp -rf '$resolvedImages/.' '$targetDir/images/'; then
                    echo "[FALLBACK_SHELL] Lệnh cp ảnh thất bại, thử dùng pipeline tar..."
                    (cd '$resolvedImages' && tar -cf - .) | (cd '$targetDir/images' && tar -xf -)
                fi
                if ! cp -f '$resolvedToken' '$targetDir/theme/token.json'; then
                    echo "[FALLBACK_SHELL] Lệnh cp token thất bại, thử dùng cat..."
                    cat '$resolvedToken' > '$targetDir/theme/token.json'
                fi
            """.trimIndent()

            val copyCmd = Shell.cmd(copyScript).exec()
            val copyOutput = (copyCmd.out + copyCmd.err).filter { it.isNotBlank() }.joinToString("\n")
            if (copyOutput.isNotBlank()) {
                log("[Shell] $copyOutput")
            }

            // Check if files actually arrived in targetDir
            val countRes = Shell.cmd("ls -1 '$targetDir/images/'*.png 2>/dev/null | wc -l").exec()
            val imgCount = countRes.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
            val tokenRes = Shell.cmd("test -f '$targetDir/theme/token.json' && echo 1 || echo 0").exec()
            val hasToken = tokenRes.out.firstOrNull()?.trim() == "1"

            if (copyCmd.isSuccess && imgCount > 0 && hasToken) {
                copySucceeded = true
                log("[Thông tin] Đã sao chép thành công $imgCount hình ảnh và token.json.")
            } else {
                log("[Cảnh báo] Lớp sao chép file từ shell chưa hoàn tất (Ảnh: $imgCount, Token: $hasToken). Đang chuyển sang cơ chế nạp trực tiếp qua dòng dữ liệu (Direct Tar Stream)...")
                val streamTarRes = streamTarToTarget(sourceImages, "$targetDir/images")
                val streamTokenRes = streamFileToTarget(tokenFile, "$targetDir/theme/token.json")

                if (streamTarRes.isSuccess && streamTokenRes.isSuccess) {
                    val recheckCount = Shell.cmd("ls -1 '$targetDir/images/'*.png 2>/dev/null | wc -l").exec().out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
                    val recheckToken = Shell.cmd("test -f '$targetDir/theme/token.json' && echo 1 || echo 0").exec().out.firstOrNull()?.trim() == "1"
                    if (recheckCount > 0 && recheckToken) {
                        copySucceeded = true
                        log("[Thành công] Đã nạp thành công $recheckCount hình ảnh và token.json qua Direct Tar Stream!")
                    }
                }

                if (!copySucceeded) {
                    val errDetails = listOfNotNull(
                        copyOutput.ifBlank { null },
                        streamTarRes.exceptionOrNull()?.message,
                        streamTokenRes.exceptionOrNull()?.message
                    ).joinToString("\n")
                    val err = "Lỗi sao chép tập tin vào MB Bank (mã lỗi ${copyCmd.code}):\n${errDetails.ifBlank { "Không thể sao chép qua shell lẫn dòng dữ liệu stream." }}"
                    log("[Lỗi] $err")
                    return@withContext InjectResult(false, logs, err)
                }
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

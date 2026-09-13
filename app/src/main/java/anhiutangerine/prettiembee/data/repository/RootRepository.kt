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

class RootRepository(
    private val context: Context,
    val targetPackage: String = "com.mbmobile"
) {
    init {
        require(isValidPackageName(targetPackage)) { "Tên gói ứng dụng không hợp lệ" }
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
                if (isSafeDataDir(extracted)) {
                    cachedDataDir = extracted
                    return@withContext extracted
                }
            }
        } catch (_: Exception) {}

        throw IllegalStateException("Không tìm thấy thư mục dữ liệu hợp lệ của $targetPackage")
    }

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
                if (!isValidUuid(cleanUuid)) continue

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
            if (!isValidUuid(config.targetUuid)) {
                val err = "UUID theme đích không hợp lệ"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }
            log("[Chuẩn bị] Bắt đầu nạp theme: ${config.sourceTheme.name}")
            log("[Thông tin] Ứng dụng mục tiêu: $targetPackage")

            // 1. Force stop MB Bank
            log("[Tiến trình] Dừng ứng dụng $targetPackage...")
            val stopResult = Shell.cmd("am force-stop $targetPackage").exec()
            if (!stopResult.isSuccess) {
                val err = "Không thể dừng ứng dụng mục tiêu (mã lỗi ${stopResult.code})"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 2. Resolve Data Directory
            val dataDir = resolveDataDir()
            log("[Thông tin] Thư mục dữ liệu: $dataDir")

            // 3. Query UID/GID with multi-tier fallback
            log("[Tiến trình] Kiểm tra định danh ứng dụng (UID/GID)...")
            val uidGid = resolveUidGid(dataDir)

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

            val missing = mutableListOf<String>()
            if (!sourceImages.isDirectory) missing += "images/"
            else if (sourceImages.listFiles()?.any { it.isFile && it.extension.equals("png", true) } != true) missing += "images/ (không có PNG)"
            if (!tokenFile.isFile) {
                missing += if (config.usePriorityVariant && !File(sourceThemeFolder, "token_priority.json").exists()) {
                    "theme/token.json"
                } else {
                    "theme/${tokenFile.name}"
                }
            }
            if (missing.isNotEmpty()) {
                val err = "Thiếu tài nguyên theme tại ${themeDir.absolutePath} (thiếu: ${missing.joinToString()})"
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
            val prepCmd = Shell.cmd(
                "mkdir -p '$targetDir'",
                "rm -rf '$targetDir/images' '$targetDir/theme'",
                "mkdir -p '$targetDir/images'",
                "mkdir -p '$targetDir/theme'"
            ).exec()
            val prepOutput = (prepCmd.out + prepCmd.err).filter { it.isNotBlank() }.joinToString("\n")
            if (prepOutput.isNotBlank()) {
                log("[Shell Prep] $prepOutput")
            }

            // Verify target directory exists
            val testTarget = Shell.cmd("test -d '$targetDir' && echo 1 || echo 0").exec()
            val targetExists = testTarget.out.firstOrNull()?.trim() == "1"

            if (!prepCmd.isSuccess || !targetExists) {
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
            val permissions = Shell.cmd(
                "chown -R $uidGid '$targetDir'",
                "chmod 755 '$dataDir/app_flutter' 2>/dev/null",
                "chmod 755 '$dataDir/app_flutter/app_theme' 2>/dev/null",
                "chmod 755 '$dataDir/app_flutter/app_theme/unzip' 2>/dev/null",
                "find '$targetDir' -type d -exec chmod 755 {} +",
                "find '$targetDir' -type f -exec chmod 644 {} +"
            ).exec()
            if (!permissions.isSuccess) {
                val err = "Không thể sửa quyền sở hữu/tập tin theme (mã lỗi ${permissions.code})"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 8. Restore SELinux context
            log("[Tiến trình] Phục hồi ngữ cảnh SELinux (restorecon)...")
            val restore = Shell.cmd("restorecon -R '$targetDir'").exec()
            if (!restore.isSuccess) {
                val err = "Không thể phục hồi ngữ cảnh SELinux (mã lỗi ${restore.code})"
                log("[Lỗi] $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 9. Post-actions ready
            log("[Thông tin] Đã phân quyền và kiểm tra tệp tin hoàn tất.")
            log("[Thành công] Nạp theme thành công. Sẵn sàng khởi chạy MB Bank.")

            return@withContext InjectResult(true, logs)
        } catch (e: Exception) {
            val err = "Ngoại lệ: ${e.message}"
            log("[Lỗi] $err")
            return@withContext InjectResult(false, logs, err)
        }
    }

    suspend fun launchMbBank(useDeeplink: Boolean = false, targetUuid: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
        try {
            if (useDeeplink && !targetUuid.isNullOrBlank()) {
                require(isValidUuid(targetUuid)) { "UUID theme đích không hợp lệ" }
                val deeplink = "mbbank://installingnew?af_force_deeplink=true&ad_dp=theme_detail&id=$targetUuid"
                val result = Shell.cmd("am start -a android.intent.action.VIEW -d '$deeplink' $targetPackage").exec()
                if (!result.isSuccess) error("Không thể mở deeplink (mã lỗi ${result.code})")
            } else {
                val result = Shell.cmd("am start -n $targetPackage/io.flutter.plugins.MainActivity").exec()
                if (!result.isSuccess) error("Không thể mở ứng dụng (mã lỗi ${result.code})")
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun resolveUidGid(dataDir: String): String? = withContext(Dispatchers.IO) {
        // Strategy 1: stat -c '%u:%g' on dataDir
        val uidRes = Shell.cmd("stat -c '%u:%g' '$dataDir' 2>/dev/null").exec()
        val statOut = uidRes.out.firstOrNull()?.trim()
        if (!statOut.isNullOrBlank() && statOut.contains(":") && !statOut.startsWith("0:0")) {
            return@withContext statOut
        }

        // Strategy 2: pm list packages -U
        val pmRes = Shell.cmd("pm list packages -U $targetPackage 2>/dev/null").exec()
        val match = pmRes.out.firstOrNull { it.contains("uid:") }
        val uid = match?.substringAfter("uid:")?.trim()?.takeWhile { it.isDigit() }
        if (!uid.isNullOrBlank()) {
            return@withContext "$uid:$uid"
        }

        // Strategy 3: dumpsys package userId=
        val dumpRes = Shell.cmd("dumpsys package $targetPackage 2>/dev/null | grep -m 1 'userId='").exec()
        val userId = dumpRes.out.firstOrNull()?.substringAfter("userId=")?.trim()?.takeWhile { it.isDigit() }
        if (!userId.isNullOrBlank()) {
            return@withContext "$userId:$userId"
        }

        null
    }

    suspend fun resetAllThemes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isRootAvailable()) {
                return@withContext Result.failure(Exception("Ứng dụng chưa được cấp quyền Root!"))
            }

            // 1. Force stop MB Bank
            val stopped = Shell.cmd("am force-stop $targetPackage").exec()
            if (!stopped.isSuccess) return@withContext Result.failure(Exception("Không thể dừng ứng dụng mục tiêu"))

            // 2. Resolve dataDir and paths
            val dataDir = resolveDataDir()
            val flutterDir = "$dataDir/app_flutter"
            val themeBase = "$flutterDir/app_theme"
            val unzipDir = "$themeBase/unzip"
            val uidGid = resolveUidGid(dataDir)

            // 3. Clear all custom themes and prepare clean structure
            val cmds = mutableListOf<String>()
            cmds.add("am force-stop $targetPackage")
            cmds.add("rm -rf '$themeBase'")
            cmds.add("rm -rf '/data/data/$targetPackage/app_flutter/app_theme' 2>/dev/null || true")
            cmds.add("rm -rf '/data/user/0/$targetPackage/app_flutter/app_theme' 2>/dev/null || true")
            cmds.add("mkdir -p '$unzipDir'")
            if (uidGid.isNullOrBlank()) return@withContext Result.failure(Exception("Không thể xác định UID/GID ứng dụng"))
            cmds.add("chown -R $uidGid '$themeBase'")
            cmds.add("chmod 755 '$dataDir' 2>/dev/null || true")
            cmds.add("chmod 755 '$flutterDir' 2>/dev/null || true")
            cmds.add("chmod 755 '$themeBase' 2>/dev/null || true")
            cmds.add("chmod 755 '$unzipDir' 2>/dev/null || true")
            cmds.add("restorecon -R '$themeBase'")

            val res = Shell.cmd(*cmds.toTypedArray()).exec()

            // 4. Verify target directory exists and is a directory
            val checkRes = Shell.cmd("test -d '$unzipDir' && echo 1 || echo 0").exec()
            val targetExists = checkRes.out.firstOrNull()?.trim() == "1"

            if (!res.isSuccess || !targetExists) {
                val errDetails = (res.out + res.err).filter { it.isNotBlank() }.joinToString("\n")
                return@withContext Result.failure(
                    Exception("Không thể khởi tạo thư mục rỗng cho theme (mã lỗi ${res.code}): ${errDetails.ifBlank { "Lệnh mkdir không thể tạo $unzipDir" }}")
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
        private val UUID = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

        fun isValidPackageName(value: String): Boolean = PACKAGE.matches(value)
        fun isValidUuid(value: String): Boolean = UUID.matches(value)
    }

    private fun isSafeDataDir(value: String): Boolean =
        listOf("/data/data/$targetPackage", "/data/user/0/$targetPackage", "/data/user_de/0/$targetPackage")
            .contains(value)
}

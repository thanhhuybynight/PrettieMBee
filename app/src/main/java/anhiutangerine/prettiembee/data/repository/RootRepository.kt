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
                    lastError = Exception(context.getString(anhiutangerine.prettiembee.R.string.log_tar_exit, exitCode, err))
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception(context.getString(anhiutangerine.prettiembee.R.string.log_tar_su_failed)))
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
                    lastError = Exception(context.getString(anhiutangerine.prettiembee.R.string.log_cat_exit, exitCode, err))
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception(context.getString(anhiutangerine.prettiembee.R.string.log_cat_su_failed)))
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

        val r = anhiutangerine.prettiembee.R.string
        try {
            log("${context.getString(r.log_tag_prepare)} ${context.getString(r.log_start_inject, config.sourceTheme.name)}")
            log("${context.getString(r.log_tag_info)} ${context.getString(r.log_target_app, targetPackage)}")

            // 1. Force stop MB Bank
            log("${context.getString(r.log_tag_progress)} ${context.getString(r.log_force_stop, targetPackage)}")
            Shell.cmd("am force-stop $targetPackage").exec()

            // 2. Resolve Data Directory
            val dataDir = resolveDataDir()
            log("${context.getString(r.log_tag_info)} ${context.getString(r.log_data_dir, dataDir)}")

            // 3. Query UID/GID with multi-tier fallback
            log("${context.getString(r.log_tag_progress)} ${context.getString(r.log_check_uid)}")
            val uidGid = resolveUidGid(dataDir)

            if (uidGid.isNullOrBlank() || !uidGid.contains(":")) {
                val err = context.getString(r.error_no_uid_gid, targetPackage)
                log("${context.getString(r.log_tag_error)} $err")
                return@withContext InjectResult(false, logs, err)
            }
            log("${context.getString(r.log_tag_info)} ${context.getString(r.log_uid_gid, uidGid)}")

            // 4. Verify source theme assets on disk
            val sourceImages = File(themeDir, "images")
            val sourceThemeFolder = File(themeDir, "theme")
            val tokenFile = if (config.usePriorityVariant && File(sourceThemeFolder, "token_priority.json").exists()) {
                log("${context.getString(r.log_tag_progress)} ${context.getString(r.log_apply_priority)}")
                File(sourceThemeFolder, "token_priority.json")
            } else {
                File(sourceThemeFolder, "token.json")
            }

            val missing = mutableListOf<String>()
            if (!sourceImages.isDirectory) missing += "images/"
            else if (sourceImages.listFiles()?.any { it.isFile } != true) missing += context.getString(r.log_images_empty)
            if (!tokenFile.isFile) {
                missing += if (config.usePriorityVariant && !File(sourceThemeFolder, "token_priority.json").exists()) {
                    "theme/token.json"
                } else {
                    "theme/${tokenFile.name}"
                }
            }
            if (missing.isNotEmpty()) {
                val err = context.getString(r.error_missing_theme_assets, themeDir.absolutePath, missing.joinToString())
                log("${context.getString(r.log_tag_error)} $err")
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
            log("${context.getString(r.log_tag_progress)} ${context.getString(r.log_prep_target, targetDir)}")
            val prepCmd = Shell.cmd(
                "mkdir -p '$targetDir'",
                "rm -rf '$targetDir/images' '$targetDir/theme'",
                "mkdir -p '$targetDir/images'",
                "mkdir -p '$targetDir/theme'"
            ).exec()
            val prepOutput = (prepCmd.out + prepCmd.err).filter { it.isNotBlank() }.joinToString("\n")
            if (prepOutput.isNotBlank()) {
                log("${context.getString(r.log_tag_shell_prep)} $prepOutput")
            }

            // Verify target directory exists
            val testTarget = Shell.cmd("test -d '$targetDir' && echo 1 || echo 0").exec()
            val targetExists = testTarget.out.firstOrNull()?.trim() == "1"

            if (!targetExists) {
                val err = context.getString(
                    r.log_mkdir_failed,
                    prepOutput.ifBlank { context.getString(r.log_mkdir_blank, targetDir) }
                )
                log("${context.getString(r.log_tag_error)} $err")
                return@withContext InjectResult(false, logs, err)
            }

            // 6. Copy from source to target using root with fallback stream
            log("${context.getString(r.log_tag_progress)} ${context.getString(r.log_copy_assets)}")
            val resolvedImages = resolveAppPath(sourceImages)
            val resolvedToken = resolveAppPath(tokenFile)
            log("${context.getString(r.log_tag_info)} ${context.getString(r.log_src_images, resolvedImages)}")
            log("${context.getString(r.log_tag_info)} ${context.getString(r.log_src_token, resolvedToken)}")

            var copySucceeded = false
            val copyScript = """
                mkdir -p '$targetDir/images'
                mkdir -p '$targetDir/theme'
                if ! cp -rf '$resolvedImages/.' '$targetDir/images/'; then
                    echo "${context.getString(r.log_fallback_images)}"
                    (cd '$resolvedImages' && tar -cf - .) | (cd '$targetDir/images' && tar -xf -)
                fi
                if ! cp -f '$resolvedToken' '$targetDir/theme/token.json'; then
                    echo "${context.getString(r.log_fallback_token)}"
                    cat '$resolvedToken' > '$targetDir/theme/token.json'
                fi
            """.trimIndent()

            val copyCmd = Shell.cmd(copyScript).exec()
            val copyOutput = (copyCmd.out + copyCmd.err).filter { it.isNotBlank() }.joinToString("\n")
            if (copyOutput.isNotBlank()) {
                log("${context.getString(r.log_tag_shell)} $copyOutput")
            }

            // Check if files actually arrived in targetDir
            val countRes = Shell.cmd("ls -1 '$targetDir/images/'*.png 2>/dev/null | wc -l").exec()
            val imgCount = countRes.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
            val tokenRes = Shell.cmd("test -f '$targetDir/theme/token.json' && echo 1 || echo 0").exec()
            val hasToken = tokenRes.out.firstOrNull()?.trim() == "1"

            if (copyCmd.isSuccess && imgCount > 0 && hasToken) {
                copySucceeded = true
                log("${context.getString(r.log_tag_info)} ${context.getString(r.log_copy_ok, imgCount)}")
            } else {
                log("${context.getString(r.log_tag_warning)} ${context.getString(r.log_copy_fallback, imgCount, hasToken)}")
                val streamTarRes = streamTarToTarget(sourceImages, "$targetDir/images")
                val streamTokenRes = streamFileToTarget(tokenFile, "$targetDir/theme/token.json")

                if (streamTarRes.isSuccess && streamTokenRes.isSuccess) {
                    val recheckCount = Shell.cmd("ls -1 '$targetDir/images/'*.png 2>/dev/null | wc -l").exec().out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
                    val recheckToken = Shell.cmd("test -f '$targetDir/theme/token.json' && echo 1 || echo 0").exec().out.firstOrNull()?.trim() == "1"
                    if (recheckCount > 0 && recheckToken) {
                        copySucceeded = true
                        log("${context.getString(r.log_tag_success)} ${context.getString(r.log_stream_ok, recheckCount)}")
                    }
                }

                if (!copySucceeded) {
                    val errDetails = listOfNotNull(
                        copyOutput.ifBlank { null },
                        streamTarRes.exceptionOrNull()?.message,
                        streamTokenRes.exceptionOrNull()?.message
                    ).joinToString("\n")
                    val err = context.getString(
                        r.log_copy_failed,
                        copyCmd.code,
                        errDetails.ifBlank { context.getString(r.log_copy_failed_blank) }
                    )
                    log("${context.getString(r.log_tag_error)} $err")
                    return@withContext InjectResult(false, logs, err)
                }
            }

            // 7. Fix permissions and ownership recursively for Flutter themes
            log("${context.getString(r.log_tag_progress)} ${context.getString(r.log_chown, uidGid)}")
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
            log("${context.getString(r.log_tag_progress)} ${context.getString(r.log_restorecon)}")
            Shell.cmd("restorecon -R '$flutterDir'").exec()

            // 9. Post-actions ready
            log("${context.getString(r.log_tag_info)} ${context.getString(r.log_perms_done)}")
            log("${context.getString(r.log_tag_success)} ${context.getString(r.log_inject_success)}")

            return@withContext InjectResult(true, logs)
        } catch (e: Exception) {
            val err = context.getString(r.log_exception, e.message ?: "")
            log("${context.getString(r.log_tag_error)} $err")
            return@withContext InjectResult(false, logs, err)
        }
    }

    fun launchMbBank(useDeeplink: Boolean = false, targetUuid: String? = null) {
        try {
            if (useDeeplink && !targetUuid.isNullOrBlank()) {
                val deeplink = "mbbank://installingnew?af_force_deeplink=true&ad_dp=theme_detail&id=$targetUuid"
                Shell.cmd("am start -a android.intent.action.VIEW -d '$deeplink' $targetPackage").exec()
            } else {
                Shell.cmd("am start -n $targetPackage/io.flutter.plugins.MainActivity").exec()
            }
        } catch (_: Exception) {}
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

        // Strategy 4: stat on APK path
        val pathRes = Shell.cmd("pm path $targetPackage 2>/dev/null").exec()
        val apkPath = pathRes.out.firstOrNull { it.startsWith("package:") }?.substringAfter("package:")?.trim()
        if (!apkPath.isNullOrBlank()) {
            val apkStat = Shell.cmd("stat -c '%u:%g' '$apkPath' 2>/dev/null").exec()
            val resUid = apkStat.out.firstOrNull()?.trim()
            if (!resUid.isNullOrBlank() && resUid.contains(":")) {
                return@withContext resUid
            }
        }

        null
    }

    suspend fun resetAllThemes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isRootAvailable()) {
                return@withContext Result.failure(Exception(context.getString(anhiutangerine.prettiembee.R.string.error_root_required)))
            }

            // 1. Force stop MB Bank
            Shell.cmd("am force-stop $targetPackage").exec()

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
            if (!uidGid.isNullOrBlank()) {
                cmds.add("chown -R $uidGid '$flutterDir' 2>/dev/null || true")
                cmds.add("chown -R $uidGid '$themeBase' 2>/dev/null || true")
            }
            cmds.add("chmod 755 '$dataDir' 2>/dev/null || true")
            cmds.add("chmod 755 '$flutterDir' 2>/dev/null || true")
            cmds.add("chmod 755 '$themeBase' 2>/dev/null || true")
            cmds.add("chmod 755 '$unzipDir' 2>/dev/null || true")
            cmds.add("restorecon -R '$themeBase' 2>/dev/null || true")

            val res = Shell.cmd(*cmds.toTypedArray()).exec()

            // 4. Verify target directory exists and is a directory
            val checkRes = Shell.cmd("test -d '$unzipDir' && echo 1 || echo 0").exec()
            val targetExists = checkRes.out.firstOrNull()?.trim() == "1"

            if (!targetExists) {
                val errDetails = (res.out + res.err).filter { it.isNotBlank() }.joinToString("\n")
                return@withContext Result.failure(
                    Exception(
                        context.getString(
                            anhiutangerine.prettiembee.R.string.log_reset_failed,
                            res.code,
                            errDetails.ifBlank {
                                context.getString(anhiutangerine.prettiembee.R.string.log_reset_mkdir_blank, unzipDir)
                            }
                        )
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


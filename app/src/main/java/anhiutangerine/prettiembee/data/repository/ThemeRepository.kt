package anhiutangerine.prettiembee.data.repository

import android.content.Context
import android.net.Uri
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.data.model.MbStoreTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.ZipInputStream

class ThemeRepository(
    private val context: Context,
    private val catalogUrl: String = REMOTE_CATALOG_URL
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val customThemesFile: File
        get() = File(context.filesDir, "custom_themes.json")

    private var cachedStoreThemes: List<MbStoreTheme>? = null

    fun getThemesDirectory(): File {
        return File(context.filesDir, "themes").apply { mkdirs() }
    }

    fun getThemeDir(themeId: String): File {
        require(isSafeThemeId(themeId)) { "ID theme không hợp lệ" }
        val root = getThemesDirectory().canonicalFile
        require(root == File(context.filesDir.canonicalFile, "themes")) { "Thư mục theme không an toàn" }
        val dir = File(root, themeId)
        require(dir.canonicalFile == dir) { "Thư mục theme không an toàn" }
        return dir
    }

    fun isThemeDownloaded(theme: CommunityTheme): Boolean {
        return try {
            hasValidThemeLayout(getThemeDir(theme.id))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    fun deleteDownloadedTheme(theme: CommunityTheme): Result<Unit> {
        return try {
            val dir = getThemeDir(theme.id)
            deleteThemeDirectory(dir)
            if (theme.isCustomImport) {
                removeFromCustomThemes(theme.id)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isSafeThemeId(id: String): Boolean = id.matches(Regex("[A-Za-z0-9_-][A-Za-z0-9._-]*"))

    private fun deleteThemeDirectory(dir: File) {
        // Recheck containment at deletion time, including symlinks inside a theme.
        require(getThemeDir(dir.name) == dir.canonicalFile) { "Thư mục theme không an toàn" }
        val prefix = dir.canonicalPath + File.separator
        dir.walkTopDown().forEach { file ->
            require(file == dir || file.canonicalPath.startsWith(prefix)) { "Đường dẫn theme không an toàn" }
            require(file.canonicalFile == file.absoluteFile) { "Liên kết theme không an toàn" }
        }
        if (dir.exists() && !dir.deleteRecursively()) throw IOException("Không thể xoá theme: ${dir.name}")
    }

    private fun removeFromCustomThemes(themeId: String) {
        if (!customThemesFile.exists()) return
        val current = loadCustomThemes().filterNot { it.id == themeId }
        customThemesFile.writeText(json.encodeToString(current))
    }

    private fun hasValidThemeLayout(themeDir: File): Boolean {
        val tokenFile = File(File(themeDir, "theme"), "token.json")
        val imagesDir = File(themeDir, "images")
        return tokenFile.isFile && imagesDir.isDirectory &&
            (imagesDir.listFiles()?.any { it.isFile && it.name.endsWith(".png") } == true)
    }

    companion object {
        const val REMOTE_CATALOG_URL = "https://raw.githubusercontent.com/thanhhuybynight/PrettieMBee/theme/community_catalog.json"
    }

    private val remoteCatalogCacheFile: File
        get() = File(context.filesDir, "remote_community_catalog.json")

    suspend fun getCommunityThemes(forceRefresh: Boolean = false): List<CommunityTheme> = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val cached = readCommunityCatalog { remoteCatalogCacheFile.readText() }
        // Ordinary reads prefer validated disk cache; explicit refresh always tries remote.
        val baseList = if (!forceRefresh && cached != null) cached else fetchCommunityCatalog()
            ?: cached
            ?: readCommunityCatalog {
                context.assets.open("community_catalog.json").bufferedReader().use { it.readText() }
            }
            ?: emptyList()
        currentCoroutineContext().ensureActive()
        baseList + loadCustomThemes()
    }

    private fun readCommunityCatalog(read: () -> String): List<CommunityTheme>? = try {
        json.decodeFromString<List<CommunityTheme>>(read()).filter { isSafeThemeId(it.id) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun fetchCommunityCatalog(): List<CommunityTheme>? {
        try {
            val conn = openHttpConnection(catalogUrl, 8000, 12000)
            val fetched = try {
                conn.inputStream.use { input ->
                    ByteArrayOutputStream().use { output ->
                        copyCancellable(input, output)
                        output.toString("UTF-8")
                    }
                }
            } finally {
                conn.disconnect()
            }
            val parsed = readCommunityCatalog { fetched } ?: return null
            currentCoroutineContext().ensureActive()
            // A malformed response must never replace a previously usable cache.
            try {
                remoteCatalogCacheFile.writeText(fetched)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // A read-only cache must not discard a usable remote catalog.
            }
            return parsed
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            return null
        }
    }

    suspend fun getStoreThemes(): List<MbStoreTheme> = withContext(Dispatchers.IO) {
        cachedStoreThemes?.let { return@withContext it }
        try {
            context.assets.open("mb_store_catalog.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val content = reader.readText()
                    // Duplicate UUIDs may be valid store aliases. Keep the first label for
                    // selection/UI keys without changing the catalog's source identities.
                    val list = json.decodeFromString<List<MbStoreTheme>>(content).distinctBy { it.uuid }
                    cachedStoreThemes = list
                    list
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun downloadTheme(
        theme: CommunityTheme,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadUrl = theme.downloadUrl ?: return@withContext Result.failure(Exception("Theme không có link tải trực tiếp"))
        var tempZip: File? = null
        var partialDir: File? = null
        var completed = false
        try {
            val destDir = getThemeDir(theme.id)
            val stagingDir = getThemeDir("staging_${UUID.randomUUID()}")
            currentCoroutineContext().ensureActive()
            tempZip = File.createTempFile("theme_", ".zip", context.cacheDir)
            val conn = openHttpConnection(downloadUrl, 15000, 60000)
            try {
                val fileLength = conn.contentLengthLong
                conn.inputStream.use { input ->
                    FileOutputStream(tempZip).use { output ->
                        copyCancellable(input, output) { total ->
                            if (fileLength > 0) onProgress(total.toFloat() / fileLength)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }

            currentCoroutineContext().ensureActive()
            partialDir = stagingDir
            check(stagingDir.mkdirs()) { "Không thể tạo thư mục theme tạm" }
            unzipFile(tempZip, stagingDir)
            normalizeExtractedStructure(stagingDir)

            if (!hasValidThemeLayout(stagingDir)) {
                val listing = stagingDir
                    .listFiles()
                    ?.joinToString(limit = 8) { it.name }
                    .orEmpty()
                return@withContext Result.failure(
                    Exception(
                        "ZIP không đúng cấu trúc (cần images/*.png + theme/token.json)" +
                            if (listing.isNotBlank()) ". Nội dung sau giải nén: $listing" else ""
                    )
                )
            }

            currentCoroutineContext().ensureActive()
            publishTheme(stagingDir, destDir)
            partialDir = null
            completed = true
            Result.success(destDir)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Result.failure(e)
        } finally {
            cleanupTransfer(tempZip, partialDir.takeUnless { completed })
        }
    }

    suspend fun importCustomZip(uri: Uri, rawFileName: String): Result<CommunityTheme> = withContext(Dispatchers.IO) {
        var tempZip: File? = null
        var partialDir: File? = null
        var completed = false
        try {
            currentCoroutineContext().ensureActive()
            val cleanName = rawFileName.removeSuffix(".zip").replace("_", " ").trim()
            val customId = "custom_${UUID.randomUUID()}"
            val destDir = getThemeDir(customId)
            tempZip = File.createTempFile("theme_import_", ".zip", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZip).use { output ->
                    copyCancellable(input, output)
                }
            } ?: return@withContext Result.failure(Exception("Không thể đọc file ZIP từ bộ nhớ máy"))

            currentCoroutineContext().ensureActive()
            check(destDir.mkdir()) { "Không thể tạo thư mục theme" }
            partialDir = destDir
            unzipFile(tempZip, destDir)

            // Look for nested structures (e.g. if zip has <UUID>/images or theme-id/images)
            normalizeExtractedStructure(destDir)

            if (!hasValidThemeLayout(destDir)) {
                return@withContext Result.failure(
                    Exception("File ZIP không hợp lệ: cần images/*.png và theme/token.json")
                )
            }

            val hasPriority = File(destDir, "theme/token_priority.json").exists()

            val newTheme = CommunityTheme(
                id = customId,
                name = cleanName.ifBlank { "Theme Tuỳ Chỉnh" },
                series = "Tự nạp (Custom)",
                author = "Người dùng",
                description = "Theme nhập từ file ZIP ngoài thiết bị.",
                defaultTargetUuid = "aa3cb89a-8325-41b4-b59b-dfaea086cf80",
                supportsPriority = hasPriority,
                downloadUrl = null,
                fileSize = "Nội bộ",
                isCustomImport = true
            )

            currentCoroutineContext().ensureActive()
            saveCustomTheme(newTheme)
            completed = true
            Result.success(newTheme)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Result.failure(e)
        } finally {
            cleanupTransfer(tempZip, partialDir.takeUnless { completed })
        }
    }

    private fun cleanupTransfer(tempZip: File?, partialDir: File?) {
        // Cleanup is best effort if the filesystem becomes unavailable. Do not replace
        // the operation's failure (especially cancellation) with a cleanup exception.
        for (file in listOfNotNull(tempZip, partialDir)) {
            try {
                if (file == tempZip) {
                    if (file.exists() && !file.delete()) throw IOException("Không thể xoá ZIP tạm")
                } else {
                    deleteThemeDirectory(file)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Replaces a downloaded theme while retaining the prior valid copy on any failure. */
    private suspend fun publishTheme(stagingDir: File, destDir: File) {
        val backupDir = getThemeDir("backup_${UUID.randomUUID()}")
        var backedUp = false
        try {
            currentCoroutineContext().ensureActive()
            if (destDir.exists()) {
                check(destDir.renameTo(backupDir)) { "Không thể sao lưu theme hiện tại" }
                backedUp = true
            }
            currentCoroutineContext().ensureActive()
            check(stagingDir.renameTo(destDir)) { "Không thể công bố theme đã tải" }
            if (backedUp) {
                try {
                    deleteThemeDirectory(backupDir)
                } catch (_: Exception) {
                    // The new theme is already valid and published; stale backup cleanup
                    // must not turn a successful replacement into a reported failure.
                }
            }
        } catch (e: Exception) {
            if (!destDir.exists() && backedUp) backupDir.renameTo(destDir)
            throw e
        }
    }

    private suspend fun openHttpConnection(address: String, connectTimeout: Int, readTimeout: Int): HttpURLConnection {
        var url = URL(address)
        repeat(6) { redirects ->
            currentCoroutineContext().ensureActive()
            require(url.protocol == "http" || url.protocol == "https") { "URL theme không hợp lệ" }
            val conn = (url.openConnection() as HttpURLConnection).apply {
                this.connectTimeout = connectTimeout
                this.readTimeout = readTimeout
                instanceFollowRedirects = false
            }
            var handedOff = false
            try {
                val code = conn.responseCode
                currentCoroutineContext().ensureActive()
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    if (redirects == 5) throw IOException("Quá nhiều chuyển hướng HTTP")
                    if (location.isNullOrBlank()) throw IOException("HTTP $code thiếu Location")
                    val next = URL(url, location)
                    if (url.protocol == "https" && next.protocol != "https") {
                        throw IOException("Chuyển hướng HTTPS không an toàn")
                    }
                    url = next
                } else {
                    if (code !in 200..299) throw IOException("HTTP error $code: ${conn.responseMessage}")
                    handedOff = true
                    return conn
                }
            } finally {
                if (!handedOff) conn.disconnect()
            }
        }
        throw IOException("Quá nhiều chuyển hướng HTTP")
    }

    private suspend fun copyCancellable(input: InputStream, output: OutputStream, onBytes: (Long) -> Unit = {}) {
        val buffer = ByteArray(16384)
        var total = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            currentCoroutineContext().ensureActive()
            if (count == -1) return
            output.write(buffer, 0, count)
            total += count
            onBytes(total)
        }
    }

    private suspend fun unzipFile(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                currentCoroutineContext().ensureActive()
                // Windows-authored zips may use backslashes; Android needs '/'
                val rawName = entry.name.replace('\\', '/')
                if (rawName.isBlank() || rawName.startsWith("/") || rawName.contains("..")) {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }
                val newFile = File(destDir, rawName)
                // Security check against Zip Slip
                if (!newFile.canonicalPath.startsWith(destDir.canonicalPath + File.separator)) {
                    throw SecurityException("Zip Slip detected: ${entry.name}")
                }
                if (rawName.endsWith("/")) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        copyCancellable(zis, fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private suspend fun normalizeExtractedStructure(destDir: File) {
        unwrapNestedLayout(destDir, depth = 0)
    }

    private suspend fun unwrapNestedLayout(destDir: File, depth: Int) {
        currentCoroutineContext().ensureActive()
        if (depth > 3 || hasValidThemeLayout(destDir)) return

        // Unwrap one nested folder: <uuid-or-id>/images + <uuid-or-id>/theme
        val rootFiles = destDir.listFiles() ?: return
        for (f in rootFiles) {
            if (!f.isDirectory) continue
            val subImages = File(f, "images")
            val subTheme = File(f, "theme")
            if (!subImages.exists() && !subTheme.exists()) continue

            val imagesTarget = File(destDir, "images")
            val themeTarget = File(destDir, "theme")
            if (subImages.exists()) {
                if (imagesTarget.exists()) imagesTarget.deleteRecursively()
                subImages.renameTo(imagesTarget)
            }
            if (subTheme.exists()) {
                if (themeTarget.exists()) themeTarget.deleteRecursively()
                subTheme.renameTo(themeTarget)
            }
            if (hasValidThemeLayout(destDir)) return
        }

        // Legacy Magisk: template.bin / *.bin is itself a zip with <uuid>/...
        for (bin in rootFiles) {
            if (!bin.isFile || !bin.name.endsWith(".bin")) continue
            try {
                unzipFile(bin, destDir)
                unwrapNestedLayout(destDir, depth + 1)
                if (hasValidThemeLayout(destDir)) {
                    bin.delete()
                    return
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // not a zip payload; ignore
            }
        }
    }

    private fun loadCustomThemes(): List<CommunityTheme> {
        if (!customThemesFile.exists()) return emptyList()
        return try {
            val content = customThemesFile.readText()
            json.decodeFromString<List<CommunityTheme>>(content).filter { isSafeThemeId(it.id) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomTheme(theme: CommunityTheme) {
        val current = loadCustomThemes().toMutableList()
        current.add(0, theme)
        customThemesFile.writeText(json.encodeToString(current))
    }
}

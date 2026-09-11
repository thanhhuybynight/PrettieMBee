package anhiutangerine.prettiembee.data.repository

import android.content.Context
import android.net.Uri
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.data.model.MbStoreTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class ThemeRepository(private val context: Context) {

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
        return File(getThemesDirectory(), themeId)
    }

    fun isThemeDownloaded(theme: CommunityTheme): Boolean {
        return hasValidThemeLayout(getThemeDir(theme.id))
    }

    fun deleteDownloadedTheme(theme: CommunityTheme): Result<Unit> {
        return try {
            val dir = getThemeDir(theme.id)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            if (theme.isCustomImport) {
                removeFromCustomThemes(theme.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun removeFromCustomThemes(themeId: String) {
        if (!customThemesFile.exists()) return
        val current = loadCustomThemes().filterNot { it.id == themeId }
        customThemesFile.writeText(json.encodeToString(current))
    }

    private fun hasValidThemeLayout(themeDir: File): Boolean {
        val tokenFile = File(File(themeDir, "theme"), "token.json")
        val imagesDir = File(themeDir, "images")
        return tokenFile.isFile && imagesDir.isDirectory && (imagesDir.listFiles()?.any { it.isFile } == true)
    }

    companion object {
        const val REMOTE_CATALOG_URL = "https://raw.githubusercontent.com/thanhhuybynight/PrettieMBee/theme/community_catalog.json"
    }

    private val remoteCatalogCacheFile: File
        get() = File(context.filesDir, "remote_community_catalog.json")

    suspend fun getCommunityThemes(forceRefresh: Boolean = false): List<CommunityTheme> = withContext(Dispatchers.IO) {
        val baseList = mutableListOf<CommunityTheme>()
        var jsonContent: String? = null

        // Try fetching online catalog from GitHub theme branch
        try {
            val url = URL(REMOTE_CATALOG_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                instanceFollowRedirects = true
            }
            if (conn.responseCode in 200..299) {
                val fetched = conn.inputStream.bufferedReader().use { it.readText() }
                if (fetched.isNotBlank()) {
                    jsonContent = fetched
                    try {
                        remoteCatalogCacheFile.writeText(fetched)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            // Network failed or offline, fallback to cache or asset
        }

        // If remote fetch failed, use cached remote catalog
        if (jsonContent == null && remoteCatalogCacheFile.exists()) {
            try {
                jsonContent = remoteCatalogCacheFile.readText()
            } catch (_: Exception) {}
        }

        // Fallback to local asset if still null
        if (jsonContent == null) {
            try {
                context.assets.open("community_catalog.json").use { stream ->
                    InputStreamReader(stream).use { reader ->
                        jsonContent = reader.readText()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        jsonContent?.let { content ->
            try {
                baseList.addAll(json.decodeFromString<List<CommunityTheme>>(content))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Add custom imported themes
        val customList = loadCustomThemes()
        baseList.addAll(customList)
        baseList
    }

    suspend fun getStoreThemes(): List<MbStoreTheme> = withContext(Dispatchers.IO) {
        cachedStoreThemes?.let { return@withContext it }
        try {
            context.assets.open("mb_store_catalog.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val content = reader.readText()
                    val list = json.decodeFromString<List<MbStoreTheme>>(content)
                    cachedStoreThemes = list
                    list
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun downloadTheme(
        theme: CommunityTheme,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadUrl = theme.downloadUrl ?: return@withContext Result.failure(Exception(context.getString(anhiutangerine.prettiembee.R.string.error_no_download_url)))
        try {
            val destDir = getThemeDir(theme.id)
            // Wipe any leftover Magisk junk / partial extracts before unpack
            if (destDir.exists()) destDir.deleteRecursively()
            destDir.mkdirs()

            val tempZip = File(context.cacheDir, "${theme.id}_temp.zip")
            var currentUrl = downloadUrl
            var conn: HttpURLConnection? = null
            var redirectCount = 0

            while (redirectCount < 5) {
                val url = URL(currentUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                conn.instanceFollowRedirects = true

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == 307 || code == 308) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val finalConn = conn ?: return@withContext Result.failure(Exception(context.getString(anhiutangerine.prettiembee.R.string.error_cannot_connect)))
            if (finalConn.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP error ${finalConn.responseCode}: ${finalConn.responseMessage}"))
            }

            val fileLength = finalConn.contentLengthLong.takeIf { it > 0 } ?: finalConn.contentLength.toLong()
            finalConn.inputStream.use { input ->
                FileOutputStream(tempZip).use { output ->
                    val buffer = ByteArray(16384)
                    var total: Long = 0
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            onProgress(total.toFloat() / fileLength)
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }

            // Unpack tempZip into destDir
            unzipFile(tempZip, destDir)
            tempZip.delete()
            normalizeExtractedStructure(destDir)

            if (!hasValidThemeLayout(destDir)) {
                val listing = destDir
                    .listFiles()
                    ?.joinToString(limit = 8) { it.name }
                    .orEmpty()
                destDir.deleteRecursively()
                return@withContext Result.failure(
                    Exception(
                        if (listing.isNotBlank()) {
                            context.getString(anhiutangerine.prettiembee.R.string.error_zip_contents, listing)
                        } else {
                            context.getString(anhiutangerine.prettiembee.R.string.error_zip_structure)
                        }
                    )
                )
            }

            Result.success(destDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importCustomZip(uri: Uri, rawFileName: String): Result<CommunityTheme> = withContext(Dispatchers.IO) {
        try {
            val cleanName = rawFileName.removeSuffix(".zip").replace("_", " ").trim()
            val customId = "custom_${System.currentTimeMillis()}"
            val destDir = getThemeDir(customId)
            if (destDir.exists()) destDir.deleteRecursively()
            destDir.mkdirs()

            val tempZip = File(context.cacheDir, "${customId}_import.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZip).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception(context.getString(anhiutangerine.prettiembee.R.string.error_zip_read)))

            unzipFile(tempZip, destDir)
            tempZip.delete()

            // Look for nested structures (e.g. if zip has <UUID>/images or theme-id/images)
            normalizeExtractedStructure(destDir)

            if (!hasValidThemeLayout(destDir)) {
                destDir.deleteRecursively()
                return@withContext Result.failure(
                    Exception(context.getString(anhiutangerine.prettiembee.R.string.error_zip_invalid))
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

            saveCustomTheme(newTheme)
            Result.success(newTheme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun unzipFile(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
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
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun normalizeExtractedStructure(destDir: File) {
        unwrapNestedLayout(destDir, depth = 0)
    }

    private fun unwrapNestedLayout(destDir: File, depth: Int) {
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
            } catch (_: Exception) {
                // not a zip payload; ignore
            }
        }
    }

    private fun loadCustomThemes(): List<CommunityTheme> {
        if (!customThemesFile.exists()) return emptyList()
        return try {
            val content = customThemesFile.readText()
            json.decodeFromString<List<CommunityTheme>>(content)
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

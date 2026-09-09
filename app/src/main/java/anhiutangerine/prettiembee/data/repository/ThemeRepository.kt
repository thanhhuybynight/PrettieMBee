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
        val themeDir = getThemeDir(theme.id)
        val tokenFile = File(themeDir, "theme/token.json")
        val imagesDir = File(themeDir, "images")
        return tokenFile.exists() && imagesDir.exists() && (imagesDir.listFiles()?.isNotEmpty() == true)
    }

    suspend fun getCommunityThemes(): List<CommunityTheme> = withContext(Dispatchers.IO) {
        val baseList = mutableListOf<CommunityTheme>()
        try {
            context.assets.open("community_catalog.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val content = reader.readText()
                    baseList.addAll(json.decodeFromString<List<CommunityTheme>>(content))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        val downloadUrl = theme.downloadUrl ?: return@withContext Result.failure(Exception("Theme không có link tải trực tiếp"))
        try {
            val destDir = getThemeDir(theme.id)
            destDir.mkdirs()

            val tempZip = File(context.cacheDir, "${theme.id}_temp.zip")
            val url = URL(downloadUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true

            val fileLength = conn.contentLength
            conn.inputStream.use { input ->
                FileOutputStream(tempZip).use { output ->
                    val buffer = ByteArray(8192)
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
            destDir.mkdirs()

            val tempZip = File(context.cacheDir, "${customId}_import.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZip).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Không thể đọc file ZIP từ bộ nhớ máy"))

            unzipFile(tempZip, destDir)
            tempZip.delete()

            // Look for nested structures (e.g. if zip has <UUID>/images or theme-id/images)
            normalizeExtractedStructure(destDir)

            val tokenFile = File(destDir, "theme/token.json")
            if (!tokenFile.exists()) {
                destDir.deleteRecursively()
                return@withContext Result.failure(Exception("File ZIP không hợp lệ: thiếu file theme/token.json"))
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
                val newFile = File(destDir, entry.name)
                // Security check against Zip Slip
                if (!newFile.canonicalPath.startsWith(destDir.canonicalPath + File.separator)) {
                    throw SecurityException("Zip Slip detected: ${entry.name}")
                }
                if (entry.isDirectory) {
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
        val rootFiles = destDir.listFiles() ?: return
        // If there's a subfolder that contains 'images' or 'theme'
        for (f in rootFiles) {
            if (f.isDirectory) {
                val subImages = File(f, "images")
                val subTheme = File(f, "theme")
                if (subImages.exists() || subTheme.exists()) {
                    // Move contents of f up to destDir
                    subImages.takeIf { it.exists() }?.renameTo(File(destDir, "images"))
                    subTheme.takeIf { it.exists() }?.renameTo(File(destDir, "theme"))
                    break
                }
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

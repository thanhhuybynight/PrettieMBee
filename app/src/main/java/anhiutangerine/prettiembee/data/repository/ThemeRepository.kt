package anhiutangerine.prettiembee.data.repository

import android.content.Context
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.data.model.MbStoreTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class ThemeRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedCommunityThemes: List<CommunityTheme>? = null
    private var cachedStoreThemes: List<MbStoreTheme>? = null

    suspend fun getCommunityThemes(): List<CommunityTheme> = withContext(Dispatchers.IO) {
        cachedCommunityThemes?.let { return@withContext it }
        try {
            context.assets.open("community_catalog.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val content = reader.readText()
                    val list = json.decodeFromString<List<CommunityTheme>>(content)
                    cachedCommunityThemes = list
                    list
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
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
}

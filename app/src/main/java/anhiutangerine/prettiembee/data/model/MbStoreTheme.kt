package anhiutangerine.prettiembee.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MbStoreTheme(
    val uuid: String,
    val slug: String,
    val displayName: String,
    val description: String = ""
)

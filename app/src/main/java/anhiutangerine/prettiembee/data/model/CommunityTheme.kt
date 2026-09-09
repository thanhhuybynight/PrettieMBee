package anhiutangerine.prettiembee.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CommunityTheme(
    val id: String,
    val name: String,
    val series: String,
    val author: String = "Community",
    val description: String = "",
    val defaultTargetUuid: String,
    val supportsPriority: Boolean = false,
    val downloadUrl: String? = null,
    val fileSize: String = "",
    val isCustomImport: Boolean = false
)

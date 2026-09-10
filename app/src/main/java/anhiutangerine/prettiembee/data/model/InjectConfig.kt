package anhiutangerine.prettiembee.data.model

data class InjectConfig(
    val sourceTheme: CommunityTheme,
    val targetUuid: String,
    val usePriorityVariant: Boolean = false,
    val autoLaunchDeeplink: Boolean = true
)

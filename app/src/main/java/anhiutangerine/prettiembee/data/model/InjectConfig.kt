package anhiutangerine.prettiembee.data.model

data class InjectConfig(
    val sourceTheme: CommunityTheme,
    val targetUuid: String,
    val usePriorityVariant: Boolean = false,
    val autoLaunchMb: Boolean = true,
    val autoLaunchDeeplink: Boolean = true,
    val backupBeforeInject: Boolean = true
)

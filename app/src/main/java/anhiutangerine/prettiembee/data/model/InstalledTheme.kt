package anhiutangerine.prettiembee.data.model

data class InstalledTheme(
    val uuid: String,
    val storeTheme: MbStoreTheme?,
    val imageCount: Int,
    val hasTokenJson: Boolean
)

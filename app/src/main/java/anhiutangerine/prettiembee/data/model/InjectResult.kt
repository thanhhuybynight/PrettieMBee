package anhiutangerine.prettiembee.data.model

data class InjectResult(
    val isSuccess: Boolean,
    val logs: List<String>,
    val errorMessage: String? = null
)

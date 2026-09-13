package anhiutangerine.prettiembee.data.repository

import android.content.Context

/** Persist the chosen package and a separate target UUID for each package. */
class TargetPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("prettiembee_target_prefs", Context.MODE_PRIVATE)

    val targetPackage: String
        get() = prefs.getString("package", null)
            ?.takeIf { RootRepository.isValidPackageName(it) } ?: "com.mbmobile"

    fun savePackage(value: String) {
        require(RootRepository.isValidPackageName(value)) { "Tên gói ứng dụng không hợp lệ" }
        prefs.edit().putString("package", value).apply()
    }

    fun target(packageName: String): Pair<String, String>? {
        val uuid = prefs.getString("$packageName.uuid", null) ?: return null
        if (!uuid.matches(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))) return null
        return uuid to (prefs.getString("$packageName.name", null) ?: uuid)
    }

    fun saveTarget(packageName: String, uuid: String, name: String) {
        require(RootRepository.isValidPackageName(packageName)) { "Tên gói ứng dụng không hợp lệ" }
        require(uuid.matches(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")))
        prefs.edit().putString("$packageName.uuid", uuid).putString("$packageName.name", name).apply()
    }
}

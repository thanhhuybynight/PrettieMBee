package anhiutangerine.prettiembee

import android.app.Application
import com.topjohnwu.superuser.Shell

class PrettieMBeeApp : Application() {
    companion object {
        init {
            Shell.enableVerboseLogging = true
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR or Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(25)
            )
        }
    }
}

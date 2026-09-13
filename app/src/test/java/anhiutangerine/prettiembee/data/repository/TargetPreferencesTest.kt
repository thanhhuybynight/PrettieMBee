package anhiutangerine.prettiembee.data.repository

import android.app.Application
import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TargetPreferencesTest {
    private lateinit var context: Context
    private val uuid = "aa3cb89a-8325-41b4-b59b-dfaea086cf80"

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("prettiembee_target_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun packageAndTargetSurviveRecreationWithoutLeakingAcrossPackages() {
        TargetPreferences(context).apply {
            savePackage("com.mbmobile.clone")
            saveTarget("com.mbmobile.clone", uuid, "Chosen target")
        }
        val reloaded = TargetPreferences(context)
        assertEquals("com.mbmobile.clone", reloaded.targetPackage)
        assertEquals(uuid to "Chosen target", reloaded.target("com.mbmobile.clone"))
        assertNull(reloaded.target("com.mbmobile"))
    }

    @Test fun invalidPackageDoesNotReplaceStoredChoice() {
        val prefs = TargetPreferences(context)
        prefs.savePackage("com.mbmobile")
        assertThrows(IllegalArgumentException::class.java) { prefs.savePackage("com.mbmobile; id") }
        assertEquals("com.mbmobile", TargetPreferences(context).targetPackage)
    }

    @Test fun corruptStoredPackageAndUuidAreIgnored() {
        context.getSharedPreferences("prettiembee_target_prefs", Context.MODE_PRIVATE).edit()
            .putString("package", "../../bad")
            .putString("com.mbmobile.uuid", "../bad")
            .commit()
        val prefs = TargetPreferences(context)
        assertEquals("com.mbmobile", prefs.targetPackage)
        assertNull(prefs.target("com.mbmobile"))
    }
}

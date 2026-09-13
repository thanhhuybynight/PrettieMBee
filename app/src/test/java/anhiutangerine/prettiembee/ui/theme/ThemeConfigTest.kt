package anhiutangerine.prettiembee.ui.theme

import android.app.Application
import android.content.Context
import android.net.Uri
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeConfigTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("prettiembee_theme_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        ThemeConfig.load(context)
    }

    @Test fun failedStatusBackgroundReplacementPreservesPreviousImage() {
        val source = File(context.cacheDir, "original.jpg").apply { writeText("original bytes") }
        ThemeConfig.saveStatusCardBackground(context, Uri.fromFile(source))
        val previous = ThemeConfig.statusCardBackgroundUri!!
        ThemeConfig.saveStatusCardBackground(context, Uri.fromFile(File(context.cacheDir, "missing.jpg")))
        assertEquals(previous, ThemeConfig.statusCardBackgroundUri)
        assertEquals("original bytes", File(previous.path!!).readText())
        ThemeConfig.load(context)
        assertEquals(previous, ThemeConfig.statusCardBackgroundUri)
    }

    @Test fun failedAppBackgroundReplacementPreservesPreviousImage() {
        val source = File(context.cacheDir, "original.jpg").apply { writeText("original bytes") }
        ThemeConfig.saveAppBackground(context, Uri.fromFile(source))
        val previous = ThemeConfig.appBackgroundUri!!
        ThemeConfig.saveAppBackground(context, Uri.fromFile(File(context.cacheDir, "missing.jpg")))
        assertEquals(previous, ThemeConfig.appBackgroundUri)
        assertEquals("original bytes", File(previous.path!!).readText())
    }
}

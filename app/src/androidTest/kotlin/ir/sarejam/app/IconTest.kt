package ir.sarejam.app

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconTest {
    @Test
    fun launcherIconIsAdaptiveWithMonochromeLayer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        assertTrue("icon should be adaptive", icon is AdaptiveIconDrawable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertNotNull("themed (monochrome) layer missing", (icon as AdaptiveIconDrawable).monochrome)
        }
    }
}

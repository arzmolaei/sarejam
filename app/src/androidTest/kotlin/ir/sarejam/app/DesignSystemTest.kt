package ir.sarejam.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.sarejam.core.ui.Accent
import ir.sarejam.core.ui.DesignShowcase
import ir.sarejam.core.ui.SarejamTheme
import ir.sarejam.core.ui.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignSystemTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun everyModeAndAccentRendersRightToLeftWithSignedAmounts() {
        var direction: LayoutDirection? = null
        var mode = ThemeMode.Light
        var accent = Accent.Turquoise
        lateinit var setMode: (ThemeMode) -> Unit
        lateinit var setAccent: (Accent) -> Unit
        rule.setContent {
            val (m, mSet) = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(mode) }
            val (a, aSet) = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(accent) }
            setMode = mSet; setAccent = aSet
            SarejamTheme(m, a) {
                Probe { direction = it }
                DesignShowcase(m, a, mSet, aSet)
            }
        }
        for (m in listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.Black)) {
            for (a in Accent.entries) {
                rule.runOnIdle { setMode(m); setAccent(a) }
                rule.waitForIdle()
                assertEquals(LayoutDirection.Rtl, direction)
                rule.onNodeWithText("+ ۲۵۰٬۰۰۰٬۰۰۰ ریال").assertIsDisplayed()
                rule.onNodeWithText("− ۱٬۸۵۰٬۰۰۰ ریال").assertIsDisplayed()
            }
        }
    }
}

@Composable
private fun Probe(onDirection: (LayoutDirection) -> Unit) = onDirection(LocalLayoutDirection.current)

package ir.sarejam.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.sarejam.feature.settings.LockRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val repo = LockRepository(ApplicationProvider.getApplicationContext())

    @After fun cleanUp() = runBlocking { repo.disable() }

    @Test fun lockIsOffByDefault() {
        runBlocking { repo.disable() }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitUntil(5_000) { rule.onAllNodesWithText("نمونه طراحی").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("سرجمع قفل است").assertDoesNotExist()
        }
    }

    @Test fun enabledLockNeedsTheRightPin() {
        runBlocking { repo.enable("2468", biometric = false) }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitUntil(5_000) { rule.onAllNodesWithText("سرجمع قفل است").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("نمونه طراحی").assertDoesNotExist()
            rule.onNodeWithTag("lock_pin").performTextInput("1111")
            rule.onNodeWithText("باز کن").performClick()
            rule.onNodeWithText("رمز درست نیست").assertIsDisplayed()
            rule.onNodeWithTag("lock_pin").performTextInput("2468")
            rule.onNodeWithText("باز کن").performClick()
            rule.onNodeWithText("نمونه طراحی").assertIsDisplayed()
        }
    }

    @Test fun turningOnFromSettingsAsksForPinTwice() {
        runBlocking { repo.disable() }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitUntil(5_000) { rule.onAllNodesWithText("تنظیمات").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("تنظیمات").performClick()
            rule.onNodeWithTag("lock_switch").performClick()
            rule.onNodeWithTag("new_pin").performTextInput("1357")
            rule.onNodeWithTag("repeat_pin").performTextInput("1358")
            rule.onNodeWithText("ذخیره").performClick()
            rule.onNodeWithText("دو رمز یکی نیستند").assertIsDisplayed()
        }
    }
}

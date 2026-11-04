package ir.sarejam.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.sarejam.feature.onboarding.OnboardingStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val store = OnboardingStore(ApplicationProvider.getApplicationContext())

    @After fun done() = runBlocking { store.markDone() }

    @Test fun firstRunWalksWelcomePrivacyThenOnlyAsksForSms() {
        runBlocking { store.reset() }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitUntil(5_000) { rule.onAllNodesWithText("شروع").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("شروع").performClick()
            rule.onNodeWithText("داده‌هایت فقط روی همین گوشی است").assertExists()
            rule.onNodeWithText("ادامه").performClick()
            rule.onNodeWithText("اجازه خواندن پیامک").assertExists()
            // Skipping is honest: the app says it works with manual entry.
            rule.onNodeWithText("بعداً").performClick()
            rule.onNodeWithText("بدون اجازه پیامک، سرجمع با ثبت دستی کار می‌کند. هر وقت خواستی از تنظیمات اجازه را بده.").assertExists()
            rule.onNodeWithText("ورود به سرجمع").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("تنظیمات").fetchSemanticsNodes().isNotEmpty() }
        }
        assertTrue(runBlocking { store.done() })
    }
}

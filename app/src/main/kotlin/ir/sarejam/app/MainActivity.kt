package ir.sarejam.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import ir.sarejam.core.ui.Accent
import ir.sarejam.core.ui.DesignShowcase
import ir.sarejam.core.ui.SarejamTheme
import ir.sarejam.core.ui.ThemeMode
import ir.sarejam.feature.onboarding.OnboardingScreen
import ir.sarejam.feature.onboarding.OnboardingStore
import ir.sarejam.feature.settings.LockPolicy
import ir.sarejam.feature.settings.LockRepository
import ir.sarejam.feature.settings.LockScreen
import ir.sarejam.feature.settings.LockSettings
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val lockRepository by lazy { LockRepository(this) }
    private val onboardingStore by lazy { OnboardingStore(this) }
    private val onboardingDone = mutableStateOf<Boolean?>(null)
    private val locked = mutableStateOf(false)
    private val lockChecked = mutableStateOf(false)
    private var backgroundedAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until we know whether to show the lock, so app content never flashes.
        splash.setKeepOnScreenCondition { !lockChecked.value || onboardingDone.value == null }
        lifecycleScope.launch { onboardingDone.value = onboardingStore.done() }
        enableEdgeToEdge()
        val startMode = intent.getStringExtra(EXTRA_MODE)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
            ?: ThemeMode.System
        setContent {
            var mode by rememberSaveable { mutableStateOf(startMode) }
            var accent by rememberSaveable { mutableStateOf(Accent.Turquoise) }
            var tab by rememberSaveable { mutableStateOf(0) }
            SarejamTheme(mode = mode, accent = accent) {
                if (onboardingDone.value == false) {
                    OnboardingScreen(onFinished = { _ ->
                        lifecycleScope.launch { onboardingStore.markDone(); onboardingDone.value = true }
                    })
                } else if (locked.value) {
                    val config = currentConfig.value
                    if (config != null) LockScreen(config, this, SystemClock::elapsedRealtime) { locked.value = false }
                } else {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column {
                            Box(Modifier.weight(1f).statusBarsPadding()) {
                                // Version 0 has no real screens yet; the design sample is the start screen.
                                if (tab == 0) DesignShowcase(mode, accent, onMode = { mode = it }, onAccent = { accent = it })
                                else LockSettings(lockRepository, this@MainActivity)
                            }
                            NavigationBar(Modifier.navigationBarsPadding()) {
                                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Text("◐") }, label = { Text("نمونه طراحی") })
                                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Text("⚙") }, label = { Text("تنظیمات") })
                            }
                        }
                    }
                }
            }
        }
    }

    private val currentConfig = mutableStateOf<ir.sarejam.feature.settings.LockConfig?>(null)

    override fun onStart() {
        super.onStart()
        // Decide synchronously from the last known settings so content never shows before the lock;
        // then refresh from storage (first start, or settings changed).
        currentConfig.value?.let { cached ->
            if (LockPolicy.shouldLock(cached, backgroundedAt, SystemClock.elapsedRealtime())) locked.value = true
        }
        lifecycleScope.launch {
            val config = lockRepository.current()
            currentConfig.value = config
            if (LockPolicy.shouldLock(config, backgroundedAt, SystemClock.elapsedRealtime())) locked.value = true
            lockChecked.value = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundedAt = SystemClock.elapsedRealtime()
    }

    companion object {
        const val EXTRA_MODE = "theme_mode"
    }
}

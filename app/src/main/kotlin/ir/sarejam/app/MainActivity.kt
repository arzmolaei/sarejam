package ir.sarejam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import ir.sarejam.core.ui.Accent
import ir.sarejam.core.ui.DesignShowcase
import ir.sarejam.core.ui.SarejamTheme
import ir.sarejam.core.ui.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Version 0 has no real screens yet; the design sample is the start screen.
        val startMode = intent.getStringExtra(EXTRA_MODE)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
            ?: ThemeMode.System
        setContent {
            var mode by rememberSaveable { mutableStateOf(startMode) }
            var accent by rememberSaveable { mutableStateOf(Accent.Turquoise) }
            SarejamTheme(mode = mode, accent = accent) {
                DesignShowcase(mode, accent, onMode = { mode = it }, onAccent = { accent = it })
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "theme_mode"
    }
}

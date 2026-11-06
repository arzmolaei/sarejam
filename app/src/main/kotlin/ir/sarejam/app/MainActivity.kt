package ir.sarejam.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import ir.sarejam.core.ui.AccentColor
import ir.sarejam.core.ui.ComponentGallery
import ir.sarejam.core.ui.SarejamTheme
import ir.sarejam.core.ui.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showGallery by rememberSaveable { mutableStateOf(false) }
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            var accentColor by rememberSaveable { mutableStateOf(AccentColor.TEAL) }
            SarejamTheme(
                themeMode = if (BuildConfig.DEBUG && showGallery) themeMode else ThemeMode.SYSTEM,
                accentColor = if (BuildConfig.DEBUG && showGallery) accentColor else AccentColor.TEAL,
            ) {
                val surfaceColor = MaterialTheme.colorScheme.surface
                // Choose the higher-contrast black/white icons from the actual resolved palette.
                val darkSystemBarIcons = surfaceColor.luminance() > 0.179f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = darkSystemBarIcons
                        isAppearanceLightNavigationBars = darkSystemBarIcons
                    }
                    // API 26–28 lack platform navigation contrast enforcement. Match the
                    // navigation background to the palette so its icons remain readable.
                    if (Build.VERSION.SDK_INT < 29) {
                        @Suppress("DEPRECATION")
                        window.navigationBarColor = surfaceColor.toArgb()
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (BuildConfig.DEBUG && showGallery) {
                        BackHandler { showGallery = false }
                        ComponentGallery(
                            themeMode = themeMode,
                            accentColor = accentColor,
                            onThemeModeChange = { themeMode = it },
                            onAccentColorChange = { accentColor = it },
                            onClose = { showGallery = false },
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            if (BuildConfig.DEBUG) {
                                OutlinedButton(onClick = { showGallery = true }) {
                                    Text(stringResource(R.string.open_component_gallery))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

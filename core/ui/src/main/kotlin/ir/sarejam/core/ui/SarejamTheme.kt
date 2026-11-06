package ir.sarejam.core.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ir.sarejam.core.common.time.SolarClock
import ir.sarejam.core.common.time.SolarLocation
import java.time.Instant
import java.time.ZoneId

/** SUNSET uses an explicitly supplied city, Tehran by default; it never requests device location. */
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED, SUNSET, AUTO_TIME }
enum class AccentColor { TEAL, LAPIS, SAFFRON, POMEGRANATE, LILAC, WALLPAPER }

/** Semantic colors available to every feature, including non-color transaction cues in MoneyText. */
@Immutable
data class SarejamSemanticColors(
    val deposit: Color,
    val withdrawal: Color,
    val goal: Color,
    val onGoal: Color,
    val mutedText: Color,
)

private val LightSemanticColors = SarejamSemanticColors(
    deposit = Color(0xFF16805B), withdrawal = Color(0xFFC2413B),
    goal = Color(0xFFC98A12), onGoal = Color(0xFF241700), mutedText = Color(0xFF5B6E6A),
)
private val DarkSemanticColors = SarejamSemanticColors(
    deposit = Color(0xFF5BD19C), withdrawal = Color(0xFFF2837D),
    goal = Color(0xFFF4B740), onGoal = Color(0xFF241700), mutedText = Color(0xFF9FB3AE),
)
private val LocalSarejamColors = staticCompositionLocalOf { LightSemanticColors }

object SarejamTheme {
    val colors: SarejamSemanticColors
        @Composable get() = LocalSarejamColors.current
}

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extra_bold, FontWeight.ExtraBold),
)

private fun textStyle(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Vazirmatn, fontWeight = weight, fontSize = size.sp,
    lineHeight = (size * 1.5).sp,
)

/** Every Material text role uses a bundled font; headlineLarge is the 32/800 amount role. */
val SarejamTypography = Typography(
    displayLarge = textStyle(57, FontWeight.ExtraBold),
    displayMedium = textStyle(45, FontWeight.ExtraBold),
    displaySmall = textStyle(36, FontWeight.ExtraBold),
    headlineLarge = textStyle(32, FontWeight.ExtraBold),
    headlineMedium = textStyle(22, FontWeight.Bold),
    headlineSmall = textStyle(20, FontWeight.Bold),
    titleLarge = textStyle(22, FontWeight.Bold),
    titleMedium = textStyle(16, FontWeight.Bold),
    titleSmall = textStyle(14, FontWeight.Medium),
    bodyLarge = textStyle(14),
    bodyMedium = textStyle(14),
    bodySmall = textStyle(12),
    labelLarge = textStyle(14, FontWeight.Medium),
    labelMedium = textStyle(12, FontWeight.Medium),
    labelSmall = textStyle(11, FontWeight.Medium),
)

/** Defaults preserve SarejamTheme { ... }. Colors and fonts never require network access. */
@Composable
fun SarejamTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.TEAL,
    solarLocation: SolarLocation = SolarLocation.Tehran,
    solarTimeZone: ZoneId = ZoneId.of("Asia/Tehran"),
    content: @Composable () -> Unit,
) {
    val auto = themeMode == ThemeMode.SUNSET || themeMode == ThemeMode.AUTO_TIME
    val now = rememberClockInstant(auto)
    val zone = ZoneId.systemDefault()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SUNSET -> SolarClock.isNight(now, solarLocation, solarTimeZone)
        ThemeMode.AUTO_TIME -> isAutomaticDarkHour(now.atZone(zone).hour)
    }
    val context = LocalContext.current
    val base = if (accentColor == AccentColor.WALLPAPER && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        brandedColorScheme(dark, accentColor)
    }
    val scheme = if (themeMode == ThemeMode.AMOLED) base.copy(
        background = Color.Black,
        surface = Color(0xFF0B0F0E),
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF16201D),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0B0F0E),
        surfaceContainer = Color(0xFF0B0F0E),
        surfaceContainerHigh = Color(0xFF16201D),
        surfaceContainerHighest = Color(0xFF202D28),
    ) else base
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalSarejamColors provides if (dark) DarkSemanticColors else LightSemanticColors,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SarejamTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(16.dp), small = RoundedCornerShape(16.dp),
                medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(16.dp),
            ),
            content = content,
        )
    }
}

/** System broadcasts update foreground UI only; no timer, worker, sensor or background service. */
@Composable
private fun rememberClockInstant(enabled: Boolean): Instant {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var now by remember { mutableStateOf(Instant.now()) }
    DisposableEffect(context, lifecycleOwner, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        now = Instant.now()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { now = Instant.now() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        var registered = false
        fun start() {
            if (registered) return
            now = Instant.now()
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
            registered = true
        }
        fun stop() {
            if (registered) context.unregisterReceiver(receiver)
            registered = false
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }
    return now
}

private fun brandedColorScheme(dark: Boolean, accent: AccentColor): ColorScheme {
    val primary = when (accent) {
        AccentColor.TEAL, AccentColor.WALLPAPER -> if (dark) Color(0xFF3FC1AE) else Color(0xFF0B7A6E)
        AccentColor.LAPIS -> if (dark) Color(0xFFACC1FF) else Color(0xFF2F5BD3)
        AccentColor.SAFFRON -> if (dark) Color(0xFFF4B740) else Color(0xFFC98A12)
        AccentColor.POMEGRANATE -> if (dark) Color(0xFFF2837D) else Color(0xFFC2413B)
        AccentColor.LILAC -> if (dark) Color(0xFFD2B9FF) else Color(0xFF7A4FD1)
    }
    val onPrimary = if (dark || accent == AccentColor.SAFFRON) Color(0xFF00201C) else Color.White
    val container = when (accent) {
        AccentColor.TEAL, AccentColor.WALLPAPER -> if (dark) Color(0xFF173F37) else Color(0xFFD2EFE7)
        AccentColor.LAPIS -> if (dark) Color(0xFF193B8F) else Color(0xFFDEE5FF)
        AccentColor.SAFFRON -> if (dark) Color(0xFF584000) else Color(0xFFFFE7B8)
        AccentColor.POMEGRANATE -> if (dark) Color(0xFF70221F) else Color(0xFFFFDAD6)
        AccentColor.LILAC -> if (dark) Color(0xFF4E267D) else Color(0xFFEDDCFF)
    }
    val onContainer = if (dark) Color(0xFFF1F5F2) else Color(0xFF15231E)
    val semantic = if (dark) DarkSemanticColors else LightSemanticColors
    return if (dark) darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = container, onPrimaryContainer = onContainer,
        secondary = Color(0xFF9ED3C4), onSecondary = Color(0xFF00382C),
        secondaryContainer = Color(0xFF244B40), onSecondaryContainer = Color(0xFFB9F1E4),
        tertiary = semantic.goal, onTertiary = semantic.onGoal,
        tertiaryContainer = Color(0xFF584000), onTertiaryContainer = Color(0xFFFFDDA0),
        background = Color(0xFF0E1513), onBackground = Color(0xFFE8F1EE),
        surface = Color(0xFF16201D), onSurface = Color(0xFFE8F1EE),
        surfaceVariant = Color(0xFF253832), onSurfaceVariant = semantic.mutedText,
        surfaceTint = primary, inverseSurface = Color(0xFFE8F1EE), inverseOnSurface = Color(0xFF16201D),
        inversePrimary = Color(0xFF0B7A6E), outline = Color(0xFF849A93), outlineVariant = Color(0xFF3F534B),
        error = semantic.withdrawal, onError = Color(0xFF430704),
        errorContainer = Color(0xFF70221F), onErrorContainer = Color(0xFFFFDAD6),
        surfaceDim = Color(0xFF0E1513), surfaceBright = Color(0xFF293A33),
        surfaceContainerLowest = Color(0xFF0A100E), surfaceContainerLow = Color(0xFF121C18),
        surfaceContainer = Color(0xFF16201D), surfaceContainerHigh = Color(0xFF202D28),
        surfaceContainerHighest = Color(0xFF293A33),
    ) else lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = container, onPrimaryContainer = onContainer,
        secondary = Color(0xFF38675A), onSecondary = Color.White,
        secondaryContainer = Color(0xFFD2EFE7), onSecondaryContainer = Color(0xFF0F2A26),
        tertiary = semantic.goal, onTertiary = semantic.onGoal,
        tertiaryContainer = Color(0xFFFFE7B8), onTertiaryContainer = Color(0xFF2B1A00),
        background = Color(0xFFF6F8F7), onBackground = Color(0xFF0F2A26),
        surface = Color.White, onSurface = Color(0xFF0F2A26),
        surfaceVariant = Color(0xFFE5EEE9), onSurfaceVariant = semantic.mutedText,
        surfaceTint = primary, inverseSurface = Color(0xFF16201D), inverseOnSurface = Color(0xFFE8F1EE),
        inversePrimary = Color(0xFF3FC1AE), outline = Color(0xFF657D74), outlineVariant = Color(0xFFC4D5CC),
        error = semantic.withdrawal, onError = Color.White,
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF430704),
        surfaceDim = Color(0xFFDAE5DE), surfaceBright = Color.White,
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F5F2),
        surfaceContainer = Color(0xFFEAF1ED), surfaceContainerHigh = Color(0xFFE5EEE9),
        surfaceContainerHighest = Color(0xFFDAE5DE),
    )
}

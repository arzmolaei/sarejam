package ir.sarejam.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.sarejam.core.ui.Palette as P

val LocalSarejamColors = staticCompositionLocalOf {
    SarejamColors(P.SaffronLight, P.DepositLight, P.WithdrawLight, P.MutedLight)
}

/** Corners 16, no shadows (docs/DESIGN.md). */
private val SarejamShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
)

fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.Light -> false
    ThemeMode.Dark, ThemeMode.Black -> true
    ThemeMode.System -> systemDark
}

/**
 * The single app theme. Always right-to-left (CLAUDE.md rule 12), whatever the phone language.
 */
@Composable
fun SarejamTheme(
    mode: ThemeMode = ThemeMode.System,
    accent: Accent = Accent.Turquoise,
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark(isSystemInDarkTheme())
    val black = mode == ThemeMode.Black
    val context = LocalContext.current
    val base: ColorScheme =
        if (accent == Accent.Wallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (dark) {
            darkColorScheme(
                primary = accent.dark,
                onPrimary = P.OnTurquoiseDark,
                secondary = P.SaffronDark,
                tertiary = P.SaffronDark,
                error = P.WithdrawDark,
            )
        } else {
            lightColorScheme(
                primary = accent.light,
                onPrimary = P.OnTurquoiseLight,
                secondary = P.SaffronLight,
                tertiary = P.SaffronLight,
                error = P.WithdrawLight,
            )
        }
    val bg = when { black -> P.BackgroundBlack; dark -> P.BackgroundDark; else -> P.BackgroundLight }
    val surface = when { black -> P.SurfaceBlack; dark -> P.SurfaceDark; else -> P.SurfaceLight }
    val text = if (dark) P.TextDark else P.TextLight
    val muted = if (dark) P.MutedDark else P.MutedLight
    val scheme = base.copy(
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        onSurfaceVariant = muted,
        outlineVariant = muted.copy(alpha = 0.3f),
    )
    val extra = if (dark) {
        SarejamColors(P.SaffronDark, P.DepositDark, P.WithdrawDark, P.MutedDark)
    } else {
        SarejamColors(P.SaffronLight, P.DepositLight, P.WithdrawLight, P.MutedLight)
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalSarejamColors provides extra,
    ) {
        MaterialTheme(colorScheme = scheme, typography = SarejamTypography, shapes = SarejamShapes, content = content)
    }
}

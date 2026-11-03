package ir.sarejam.core.ui

import androidx.compose.ui.graphics.Color

/** Palette from docs/DESIGN.md. */
internal object Palette {
    val TurquoiseLight = Color(0xFF0B7A6E)
    val TurquoiseDark = Color(0xFF3FC1AE)
    val OnTurquoiseLight = Color(0xFFFFFFFF)
    val OnTurquoiseDark = Color(0xFF00201C)

    val SaffronLight = Color(0xFFC98A12)
    val SaffronDark = Color(0xFFF4B740)
    val DepositLight = Color(0xFF16805B)
    val DepositDark = Color(0xFF5BD19C)
    val WithdrawLight = Color(0xFFC2413B)
    val WithdrawDark = Color(0xFFF2837D)

    val BackgroundLight = Color(0xFFF6F8F7)
    val BackgroundDark = Color(0xFF0E1513)
    val BackgroundBlack = Color(0xFF000000)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF16201D)
    val SurfaceBlack = Color(0xFF0B0F0E)
    val TextLight = Color(0xFF0F2A26)
    val TextDark = Color(0xFFE8F1EE)
    val MutedLight = Color(0xFF5B6E6A)
    val MutedDark = Color(0xFF9FB3AE)
}

/** The user's choice of main colour (docs/DESIGN.md «تم‌ها»). */
enum class Accent(internal val light: Color, internal val dark: Color) {
    Turquoise(Palette.TurquoiseLight, Palette.TurquoiseDark),
    Lapis(Color(0xFF2F5BD3), Color(0xFF8EA8F2)),
    Saffron(Palette.SaffronLight, Palette.SaffronDark),
    Pomegranate(Palette.WithdrawLight, Palette.WithdrawDark),
    Lilac(Color(0xFF7A4FD1), Color(0xFFBBA0F2)),

    /** Wallpaper colours on Android 12+; falls back to turquoise on older phones. */
    Wallpaper(Palette.TurquoiseLight, Palette.TurquoiseDark),
}

enum class ThemeMode { Light, Dark, Black, System }

/** Colours with a fixed meaning that Material's scheme has no slot for. */
data class SarejamColors(
    val result: Color,
    val deposit: Color,
    val withdraw: Color,
    val muted: Color,
)

package ir.sarejam.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

private val Vazirmatn = FontFamily(Font(R.font.vazirmatn_regular, FontWeight.Normal))

// The complete theme system is roadmap step 0.3.
@Composable
fun SarejamTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Color(0xFF0B7A6E),
                background = Color(0xFFF6F8F7),
                surface = Color(0xFFF6F8F7),
                onSurface = Color(0xFF0F2A26),
            ),
            typography = Typography(
                headlineMedium = TextStyle(fontFamily = Vazirmatn, fontSize = 22.sp),
                bodyLarge = TextStyle(fontFamily = Vazirmatn, fontSize = 14.sp),
            ),
            content = content,
        )
    }
}

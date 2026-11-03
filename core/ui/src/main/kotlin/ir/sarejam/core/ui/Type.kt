package ir.sarejam.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, weight: FontWeight) =
    TextStyle(fontFamily = Vazirmatn, fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.5).sp)

/** Sizes from docs/DESIGN.md «فونت». */
object SarejamType {
    /** Big amounts: 32 / 800 with equal-width digits. */
    val amount = style(32, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum")
    val screenTitle = style(22, FontWeight.Bold)
    val cardTitle = style(16, FontWeight.Bold)
    val body = style(14, FontWeight.Normal)
    val caption = style(12, FontWeight.Normal)
}

internal val SarejamTypography = Typography(
    displaySmall = SarejamType.amount,
    headlineSmall = SarejamType.screenTitle,
    titleLarge = SarejamType.screenTitle,
    titleMedium = SarejamType.cardTitle,
    titleSmall = style(14, FontWeight.Bold),
    bodyLarge = style(16, FontWeight.Normal),
    bodyMedium = SarejamType.body,
    bodySmall = SarejamType.caption,
    labelLarge = style(14, FontWeight.Medium),
    labelMedium = style(12, FontWeight.Medium),
    labelSmall = style(11, FontWeight.Medium),
)

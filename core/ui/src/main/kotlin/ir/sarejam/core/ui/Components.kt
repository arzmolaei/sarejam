package ir.sarejam.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A signed, grouped rial amount. No floating point conversion or overflow, including Long.MIN_VALUE.
 * Signs and an isolated LTR numeric run remain readable in Persian and by screen readers.
 */
@Composable
fun MoneyText(
    amountRial: Long,
    modifier: Modifier = Modifier,
    usePersianDigits: Boolean = true,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = when {
        amountRial > 0 -> SarejamTheme.colors.deposit
        amountRial < 0 -> SarejamTheme.colors.withdrawal
        else -> MaterialTheme.colorScheme.onSurface
    },
) {
    val number = formatSignedAmount(amountRial, usePersianDigits)
    val currency = stringResource(R.string.sarejam_currency_rial)
    val spokenAmount = when {
        amountRial > 0 -> stringResource(R.string.sarejam_amount_positive, number.drop(1), currency)
        amountRial < 0 -> stringResource(R.string.sarejam_amount_negative, number.drop(1), currency)
        else -> stringResource(R.string.sarejam_amount_zero, number, currency)
    }
    Text(
        text = "\u2066$number\u2069 $currency",
        modifier = modifier.clearAndSetSemantics { contentDescription = spokenAmount },
        color = color,
        style = style.copy(fontFeatureSettings = "tnum"),
    )
}

/** A flat 16dp card with an 8dp grid and a visible boundary; no elevation or shadow. */
@Composable
fun SarejamCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** One sentence and one start action. The caller supplies localized resource strings. */
@Composable
fun EmptyState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onAction) { Text(actionLabel) }
    }
}

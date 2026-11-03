package ir.sarejam.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import ir.sarejam.core.common.Money

/** Card with a thin border instead of a shadow; 16 corners, padding 16. */
@Composable
fun SarejamCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

enum class Flow { Deposit, Withdraw, Neutral }

/**
 * An amount in rials. Deposits and withdrawals always carry + / − as well as colour,
 * for people who do not tell the colours apart.
 */
@Composable
fun AmountText(
    rial: Long,
    flow: Flow,
    modifier: Modifier = Modifier,
    unit: String = "ریال",
    style: TextStyle = SarejamType.amount,
) {
    val colors = LocalSarejamColors.current
    val sign = when (flow) { Flow.Deposit -> "+ "; Flow.Withdraw -> "− "; Flow.Neutral -> "" }
    val color = when (flow) {
        Flow.Deposit -> colors.deposit
        Flow.Withdraw -> colors.withdraw
        Flow.Neutral -> MaterialTheme.colorScheme.onSurface
    }
    val text = "$sign${Money.group(kotlin.math.abs(rial))} $unit"
    Text(text = text, style = style, color = color, modifier = modifier.semantics { contentDescription = text })
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) =
    Text(text, style = SarejamType.caption, color = LocalSarejamColors.current.muted, modifier = modifier)

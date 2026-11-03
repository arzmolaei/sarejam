package ir.sarejam.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Sample screen showing every shared component (ROADMAP 0.3). */
@Composable
fun DesignShowcase(
    mode: ThemeMode,
    accent: Accent,
    onMode: (ThemeMode) -> Unit,
    onAccent: (Accent) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("سرجمع", style = SarejamType.screenTitle)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.Light to "روشن", ThemeMode.Dark to "تیره", ThemeMode.Black to "مشکی").forEach { (m, label) ->
                    FilterChip(selected = mode == m, onClick = { onMode(m) }, label = { Text(label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Accent.Turquoise to "فیروزه", Accent.Lapis to "لاجورد", Accent.Saffron to "زعفران",
                ).forEach { (a, label) -> FilterChip(selected = accent == a, onClick = { onAccent(a) }, label = { Text(label) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Accent.Pomegranate to "انار", Accent.Lilac to "یاس", Accent.Wallpaper to "والپیپر",
                ).forEach { (a, label) -> FilterChip(selected = accent == a, onClick = { onAccent(a) }, label = { Text(label) }) }
            }
            SarejamCard {
                Text("خرج امروز", style = SarejamType.cardTitle)
                AmountText(4_200_000, Flow.Neutral)
                Caption("۴۲۰ هزار تومان")
            }
            SarejamCard {
                Text("آخرین تراکنش‌ها", style = SarejamType.cardTitle)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("حقوق", style = SarejamType.body, modifier = Modifier.weight(1f))
                    AmountText(250_000_000, Flow.Deposit, style = SarejamType.cardTitle)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سوپرمارکت", style = SarejamType.body, modifier = Modifier.weight(1f))
                    AmountText(1_850_000, Flow.Withdraw, style = SarejamType.cardTitle)
                }
            }
            var text by remember { mutableStateOf("") }
            OutlinedTextField(text, { text = it }, label = { Text("یادداشت") }, modifier = Modifier.padding(0.dp))
            var on by remember { mutableStateOf(true) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("سؤال «این پول برای چی بود؟»", style = SarejamType.body, modifier = Modifier.weight(1f))
                Switch(on, { on = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("ثبت") }
                OutlinedButton(onClick = {}) { Text("برگردان") }
            }
            Text("هدف پس‌انداز ۶۰٪", style = SarejamType.cardTitle, color = LocalSarejamColors.current.result)
        }
    }
}

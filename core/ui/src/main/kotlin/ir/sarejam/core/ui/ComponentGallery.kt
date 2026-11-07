package ir.sarejam.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Design tooling only. The host must gate navigation to this gallery with BuildConfig.DEBUG. */
@Composable
fun ComponentGallery(
    themeMode: ThemeMode,
    accentColor: AccentColor,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var actionDone by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        bottomBar = {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(stringResource(R.string.sarejam_gallery_close)) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.sarejam_gallery_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.sarejam_gallery_description),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item {
                SarejamCard(Modifier.fillMaxWidth()) {
                    GalleryHeading(R.string.sarejam_gallery_theme)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(stringResource(themeLabel(mode))) },
                            )
                        }
                    }
                    if (themeMode == ThemeMode.AUTO_TIME) {
                        Text(stringResource(R.string.sarejam_auto_time_description), style = MaterialTheme.typography.bodySmall)
                    }
                    if (themeMode == ThemeMode.SUNSET) {
                        Text(stringResource(R.string.sarejam_sunset_description), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                SarejamCard(Modifier.fillMaxWidth()) {
                    GalleryHeading(R.string.sarejam_gallery_accent)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AccentColor.entries.forEach { accent ->
                            FilterChip(
                                selected = accentColor == accent,
                                onClick = { onAccentColorChange(accent) },
                                label = { Text(stringResource(accentLabel(accent))) },
                            )
                        }
                    }
                    if (accentColor == AccentColor.WALLPAPER) {
                        Text(stringResource(R.string.sarejam_wallpaper_description), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                SarejamCard(Modifier.fillMaxWidth()) {
                    GalleryHeading(R.string.sarejam_gallery_amounts)
                    Text(stringResource(R.string.sarejam_gallery_deposit), style = MaterialTheme.typography.bodyLarge)
                    MoneyText(20_014_000)
                    Text(stringResource(R.string.sarejam_gallery_withdrawal), style = MaterialTheme.typography.bodyLarge)
                    MoneyText(-320_000)
                    Text(stringResource(R.string.sarejam_gallery_large_amount), style = MaterialTheme.typography.bodySmall)
                    MoneyText(Long.MAX_VALUE, style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.sarejam_gallery_english_digits), style = MaterialTheme.typography.bodySmall)
                    MoneyText(Long.MAX_VALUE, usePersianDigits = false, style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.sarejam_gallery_long_min), style = MaterialTheme.typography.bodySmall)
                    MoneyText(Long.MIN_VALUE, style = MaterialTheme.typography.bodyLarge)
                    MoneyText(0, style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                SarejamCard(Modifier.fillMaxWidth()) {
                    GalleryHeading(R.string.sarejam_gallery_typography)
                    Text(stringResource(R.string.sarejam_gallery_page_heading), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.sarejam_gallery_card_heading), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.sarejam_gallery_body), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.sarejam_gallery_medium), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.sarejam_gallery_caption), style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                SarejamCard(Modifier.fillMaxWidth()) {
                    GalleryHeading(R.string.sarejam_gallery_colors)
                    ColorSwatch(R.string.sarejam_color_primary, MaterialTheme.colorScheme.primary)
                    ColorSwatch(R.string.sarejam_color_goal, SarejamTheme.colors.goal)
                    ColorSwatch(R.string.sarejam_color_deposit, SarejamTheme.colors.deposit)
                    ColorSwatch(R.string.sarejam_color_withdrawal, SarejamTheme.colors.withdrawal)
                }
            }
            item {
                SarejamCard(Modifier.fillMaxWidth()) {
                    GalleryHeading(R.string.sarejam_gallery_buttons)
                    Button(onClick = { actionDone = !actionDone }) {
                        Text(stringResource(if (actionDone) R.string.sarejam_gallery_action_done else R.string.sarejam_gallery_button))
                    }
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text(stringResource(R.string.sarejam_gallery_disabled))
                    }
                }
            }
            item {
                SarejamCard(Modifier.fillMaxWidth()) {
                    EmptyState(
                        message = stringResource(R.string.sarejam_gallery_empty_title),
                        actionLabel = stringResource(if (actionDone) R.string.sarejam_gallery_action_done else R.string.sarejam_gallery_empty_action),
                        onAction = { actionDone = !actionDone },
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryHeading(label: Int) {
    Text(
        stringResource(label),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun ColorSwatch(label: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(32.dp).background(color, MaterialTheme.shapes.small))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.sarejam_theme_system
    ThemeMode.LIGHT -> R.string.sarejam_theme_light
    ThemeMode.DARK -> R.string.sarejam_theme_dark
    ThemeMode.AMOLED -> R.string.sarejam_theme_amoled
    ThemeMode.SUNSET -> R.string.sarejam_theme_sunset
    ThemeMode.AUTO_TIME -> R.string.sarejam_theme_auto_time
}

private fun accentLabel(accent: AccentColor): Int = when (accent) {
    AccentColor.TEAL -> R.string.sarejam_accent_teal
    AccentColor.LAPIS -> R.string.sarejam_accent_lapis
    AccentColor.SAFFRON -> R.string.sarejam_accent_saffron
    AccentColor.POMEGRANATE -> R.string.sarejam_accent_pomegranate
    AccentColor.LILAC -> R.string.sarejam_accent_lilac
    AccentColor.WALLPAPER -> R.string.sarejam_accent_wallpaper
}

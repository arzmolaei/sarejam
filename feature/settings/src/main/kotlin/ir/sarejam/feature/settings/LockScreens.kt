package ir.sarejam.feature.settings

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.sarejam.core.ui.Caption
import ir.sarejam.core.ui.SarejamCard
import ir.sarejam.core.ui.SarejamType
import kotlinx.coroutines.launch

private const val BIOMETRIC_KINDS = BiometricManager.Authenticators.BIOMETRIC_WEAK

fun canUseBiometric(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(BIOMETRIC_KINDS) == BiometricManager.BIOMETRIC_SUCCESS

private fun askBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("باز کردن سرجمع")
            .setNegativeButtonText("رمز")
            .setAllowedAuthenticators(BIOMETRIC_KINDS)
            .build(),
    )
}

/** Shown instead of the app while it is locked. */
@Composable
fun LockScreen(config: LockConfig, activity: FragmentActivity, now: () -> Long, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var failures by remember { mutableIntStateOf(0) }
    var lastFailure by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<String?>(null) }
    val biometricReady = config.biometric && canUseBiometric(activity)
    LaunchedEffect(Unit) { if (biometricReady) askBiometric(activity, onUnlock) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("سرجمع قفل است", style = SarejamType.screenTitle)
            OutlinedTextField(
                value = pin,
                onValueChange = { v -> pin = v.filter(Char::isDigit).take(8) },
                label = { Text("رمز") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.testTag("lock_pin"),
            )
            message?.let { Caption(it) }
            Button(
                onClick = {
                    val wait = LockPolicy.waitAfterFailures(failures, lastFailure, now())
                    when {
                        wait > 0 -> message = "چند لحظه صبر کن (${wait / 1000 + 1} ثانیه)"
                        config.pinHash != null && PinHasher.verify(pin, config.pinHash) -> onUnlock()
                        else -> { failures++; lastFailure = now(); pin = ""; message = "رمز درست نیست" }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("باز کن") }
            if (biometricReady) {
                OutlinedButton(onClick = { askBiometric(activity, onUnlock) }, modifier = Modifier.fillMaxWidth()) { Text("اثر انگشت") }
            }
        }
    }
}

/** The lock part of settings. Off by default; turning it on asks for a PIN twice. */
@Composable
fun LockSettings(repository: LockRepository, activity: FragmentActivity) {
    val config by repository.config.collectAsState(initial = LockConfig())
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var settingUp by remember { mutableStateOf(false) }
    var useBiometric by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val biometricAvailable = remember { canUseBiometric(activity) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("تنظیمات", style = SarejamType.screenTitle)
        SarejamCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("قفل برنامه", style = SarejamType.cardTitle)
                    Caption(if (config.enabled) "روشن" else "خاموش")
                }
                Switch(
                    checked = config.enabled || settingUp,
                    onCheckedChange = { on ->
                        if (on) settingUp = true else { settingUp = false; scope.launch { repository.disable() } }
                    },
                    modifier = Modifier.testTag("lock_switch"),
                )
            }
            if (settingUp && !config.enabled) {
                val pinField = @Composable { value: String, label: String, tag: String, set: (String) -> Unit ->
                    OutlinedTextField(
                        value = value, onValueChange = { set(it.filter(Char::isDigit).take(8)) }, label = { Text(label) },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(tag),
                    )
                }
                pinField(pin, "رمز ۴ تا ۸ رقمی", "new_pin") { pin = it }
                pinField(repeat, "تکرار رمز", "repeat_pin") { repeat = it }
                if (biometricAvailable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("باز کردن با اثر انگشت", style = SarejamType.body, modifier = Modifier.weight(1f))
                        Switch(useBiometric, { useBiometric = it })
                    }
                }
                error?.let { Caption(it) }
                Button(
                    onClick = {
                        error = when {
                            !PinHasher.isValidPin(pin) -> "رمز باید ۴ تا ۸ رقم باشد"
                            pin != repeat -> "دو رمز یکی نیستند"
                            else -> null
                        }
                        if (error == null) {
                            scope.launch { repository.enable(pin, useBiometric && biometricAvailable) }
                            settingUp = false; pin = ""; repeat = ""
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("ذخیره") }
            }
            if (config.enabled) {
                Caption("قفل خودکار بعد از:", Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "فوری", 60 to "۱ دقیقه", 300 to "۵ دقیقه").forEach { (s, label) ->
                        FilterChip(config.autoLockSeconds == s, { scope.launch { repository.setAutoLock(s) } }, { Text(label) })
                    }
                }
            }
        }
    }
}

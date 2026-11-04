package ir.sarejam.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ir.sarejam.core.ui.SarejamCard
import ir.sarejam.core.ui.SarejamType

private enum class Step { WELCOME, PRIVACY, SMS, DONE }

fun hasSmsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

private fun installer(context: Context): String? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    else @Suppress("DEPRECATION") context.packageManager.getInstallerPackageName(context.packageName)
}.getOrNull()

/** docs/PRODUCT.md «شروع کار»: welcome → privacy → only the SMS permission → ready. */
/** Progress of the 6-month inbox import (ROADMAP 1.3), fed by the app. */
data class ImportProgress(val done: Int, val total: Int, val finished: Boolean, val transactions: Int)

@Composable
fun OnboardingScreen(
    importProgress: ImportProgress? = null,
    onSmsGranted: () -> Unit = {},
    onFinished: (smsGranted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    val initialGuide = InstallEnvironment.smsGuide(Build.VERSION.SDK_INT, InstallEnvironment.source(installer(context)))
    var guide by rememberSaveable { mutableStateOf(initialGuide) }
    var granted by rememberSaveable { mutableStateOf(hasSmsPermission(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result.values.all { it }
        if (granted) { onSmsGranted(); step = Step.DONE }
        else {
            val activity = context as? Activity
            val canAskAgain = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECEIVE_SMS)
            guide = InstallEnvironment.afterDenial(canAskAgain)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.weight(1f))
            when (step) {
                Step.WELCOME -> {
                    Text(stringResource(R.string.onb_welcome_title), style = SarejamType.amount)
                    Text(stringResource(R.string.onb_welcome_body), style = SarejamType.body)
                    Spacer(Modifier.weight(1f))
                    Button({ step = Step.PRIVACY }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_start)) }
                }
                Step.PRIVACY -> {
                    Text(stringResource(R.string.onb_privacy_title), style = SarejamType.screenTitle)
                    Text(stringResource(R.string.onb_privacy_body), style = SarejamType.body)
                    SarejamCard { Text(stringResource(R.string.onb_privacy_points), style = SarejamType.body) }
                    Spacer(Modifier.weight(1f))
                    Button({ step = if (granted) Step.DONE else Step.SMS }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_next)) }
                }
                Step.SMS -> {
                    Text(stringResource(R.string.onb_sms_title), style = SarejamType.screenTitle)
                    Text(stringResource(R.string.onb_sms_body), style = SarejamType.body)
                    if (guide == SmsGuide.RESTRICTED_SETTINGS) {
                        SarejamCard {
                            Text(stringResource(R.string.onb_restricted_title), style = SarejamType.cardTitle)
                            Text(stringResource(R.string.onb_restricted_steps), style = SarejamType.body)
                            TextButton({
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }) { Text(stringResource(R.string.onb_open_app_info)) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        { launcher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)) },
                        Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.onb_sms_allow)) }
                    TextButton({ granted = hasSmsPermission(context); step = Step.DONE }, Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onb_later))
                    }
                }
                Step.DONE -> {
                    Text(stringResource(R.string.onb_done_title), style = SarejamType.screenTitle)
                    Text(stringResource(if (granted) R.string.onb_done_granted else R.string.onb_done_denied), style = SarejamType.body)
                    if (granted && importProgress != null) {
                        SarejamCard {
                            if (!importProgress.finished) {
                                Text(stringResource(R.string.onb_import_running, importProgress.done, importProgress.total), style = SarejamType.body)
                                if (importProgress.total > 0) {
                                    LinearProgressIndicator(
                                        progress = { importProgress.done.toFloat() / importProgress.total },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    )
                                } else LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                            } else {
                                Text(stringResource(R.string.onb_import_done, importProgress.transactions), style = SarejamType.body)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button({ onFinished(granted) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_enter)) }
                }
            }
        }
    }
}

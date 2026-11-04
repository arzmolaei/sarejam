package ir.sarejam.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class InstallEnvironmentTest {
    @Test fun installers() {
        assertEquals(InstallSource.DIRECT, InstallEnvironment.source(null))
        assertEquals(InstallSource.DIRECT, InstallEnvironment.source("com.google.android.packageinstaller"))
        assertEquals(InstallSource.BAZAAR, InstallEnvironment.source("com.farsitel.bazaar"))
        assertEquals(InstallSource.MYKET, InstallEnvironment.source("ir.mservices.market"))
        assertEquals(InstallSource.DIRECT, InstallEnvironment.source("com.miui.packageinstaller"))
    }

    @Test fun directApkOnAndroid15NeedsRestrictedSettingsGuide() =
        assertEquals(SmsGuide.RESTRICTED_SETTINGS, InstallEnvironment.smsGuide(35, InstallSource.DIRECT))

    @Test fun olderAndroidOrStoreUsesTheNormalDialog() {
        assertEquals(SmsGuide.NORMAL, InstallEnvironment.smsGuide(34, InstallSource.DIRECT))
        assertEquals(SmsGuide.NORMAL, InstallEnvironment.smsGuide(26, InstallSource.DIRECT))
        assertEquals(SmsGuide.NORMAL, InstallEnvironment.smsGuide(35, InstallSource.BAZAAR))
    }

    @Test fun blockedDialogFallsBackToTheGuide() =
        assertEquals(SmsGuide.RESTRICTED_SETTINGS, InstallEnvironment.afterDenial(canAskAgain = false))
}

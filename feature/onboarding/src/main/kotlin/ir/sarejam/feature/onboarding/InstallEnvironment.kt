package ir.sarejam.feature.onboarding

/** Where the app came from; decides which SMS-permission guide to show (docs/ARCHITECTURE.md). */
enum class InstallSource { DIRECT, BAZAAR, MYKET, PLAY, OTHER_STORE }

enum class SmsGuide {
    /** The normal Android permission dialog works. */
    NORMAL,
    /**
     * Android 15+ "restricted setting" (Enhanced Confirmation): the dialog is blocked until the user
     * opens App info → ⋮ → «Allow restricted settings».
     */
    RESTRICTED_SETTINGS,
}

object InstallEnvironment {
    fun source(installer: String?): InstallSource = when (installer) {
        null, "com.google.android.packageinstaller", "com.android.packageinstaller", "com.android.shell" -> InstallSource.DIRECT
        "com.farsitel.bazaar" -> InstallSource.BAZAAR
        "ir.mservices.market" -> InstallSource.MYKET
        "com.android.vending" -> InstallSource.PLAY
        else -> if (installer.contains("packageinstaller")) InstallSource.DIRECT else InstallSource.OTHER_STORE
    }

    /**
     * Guide to show up front. Android 8–14: the normal dialog. Android 15+ outside Google Play:
     * restricted (for Bazaar/Myket it depends on the phone maker, so we also fall back to it whenever
     * the dialog cannot appear — see [afterDenial]).
     */
    fun smsGuide(sdkInt: Int, source: InstallSource): SmsGuide =
        if (sdkInt >= 35 && source == InstallSource.DIRECT) SmsGuide.RESTRICTED_SETTINGS else SmsGuide.NORMAL

    /** When the dialog was refused without a chance to show it again, only the settings route is left. */
    fun afterDenial(canAskAgain: Boolean): SmsGuide = if (canAskAgain) SmsGuide.NORMAL else SmsGuide.RESTRICTED_SETTINGS
}

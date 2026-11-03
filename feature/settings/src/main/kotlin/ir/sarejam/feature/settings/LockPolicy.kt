package ir.sarejam.feature.settings

/** Pure rules for when the optional app lock applies. */
data class LockConfig(
    val enabled: Boolean = false,
    val pinHash: String? = null,
    val biometric: Boolean = false,
    /** 0 = lock as soon as the app leaves the screen. */
    val autoLockSeconds: Int = 60,
)

object LockPolicy {
    const val MAX_ATTEMPTS = 5
    const val COOLDOWN_MILLIS = 30_000L

    /** Locked on a fresh start, or when the app was away longer than the auto-lock time. */
    fun shouldLock(config: LockConfig, backgroundedAt: Long?, now: Long): Boolean {
        if (!config.enabled || config.pinHash == null) return false
        if (backgroundedAt == null) return true
        return now - backgroundedAt >= config.autoLockSeconds * 1000L
    }

    /** After too many wrong PINs, wait before the next try. Returns millis still to wait. */
    fun waitAfterFailures(failures: Int, lastFailureAt: Long, now: Long): Long {
        if (failures < MAX_ATTEMPTS) return 0
        return (lastFailureAt + COOLDOWN_MILLIS - now).coerceAtLeast(0)
    }
}

package ir.sarejam.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockTest {
    private val on = LockConfig(enabled = true, pinHash = "x", autoLockSeconds = 60)

    @Test fun lockIsOffByDefault() = assertFalse(LockPolicy.shouldLock(LockConfig(), backgroundedAt = null, now = 0))
    @Test fun freshStartLocksWhenEnabled() = assertTrue(LockPolicy.shouldLock(on, backgroundedAt = null, now = 0))
    @Test fun shortTripAwayStaysOpen() = assertFalse(LockPolicy.shouldLock(on, backgroundedAt = 0, now = 59_000))
    @Test fun longTripAwayLocks() = assertTrue(LockPolicy.shouldLock(on, backgroundedAt = 0, now = 60_000))
    @Test fun immediateLock() = assertTrue(LockPolicy.shouldLock(on.copy(autoLockSeconds = 0), backgroundedAt = 0, now = 1))
    @Test fun enabledWithoutPinNeverLocksOut() = assertFalse(LockPolicy.shouldLock(on.copy(pinHash = null), null, 0))

    @Test fun pinHashVerifiesOnlyTheRightPin() {
        val stored = PinHasher.hash("2468")
        assertTrue(PinHasher.verify("2468", stored))
        assertFalse(PinHasher.verify("2469", stored))
        assertFalse(stored.contains("2468"))
    }

    @Test fun samePinGetsDifferentSalt() = assertNotEquals(PinHasher.hash("2468"), PinHasher.hash("2468"))
    @Test fun pinRules() {
        assertFalse(PinHasher.isValidPin("123")); assertFalse(PinHasher.isValidPin("12a4")); assertTrue(PinHasher.isValidPin("1234"))
    }
    @Test fun cooldownAfterFiveFailures() {
        assertEquals(0, LockPolicy.waitAfterFailures(4, lastFailureAt = 0, now = 0))
        assertEquals(30_000, LockPolicy.waitAfterFailures(5, lastFailureAt = 0, now = 0))
        assertEquals(0, LockPolicy.waitAfterFailures(5, lastFailureAt = 0, now = 30_000))
    }
}

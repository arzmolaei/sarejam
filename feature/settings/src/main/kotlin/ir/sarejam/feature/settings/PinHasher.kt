package ir.sarejam.feature.settings

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** The PIN itself is never stored: only a salted PBKDF2 hash. */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val BITS = 256

    fun isValidPin(pin: String): Boolean = pin.length in 4..8 && pin.all { it in '0'..'9' }

    fun hash(pin: String, salt: ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }): String {
        require(isValidPin(pin)) { "PIN must be 4–8 digits" }
        val enc = Base64.getEncoder()
        return "${enc.encodeToString(salt)}:${enc.encodeToString(derive(pin, salt))}"
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 2 || !isValidPin(pin)) return false
        val dec = Base64.getDecoder()
        return MessageDigest.isEqual(derive(pin, dec.decode(parts[0])), dec.decode(parts[1]))
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, BITS)).encoded
}

package ir.sarejam.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The database passphrase: 32 random bytes, stored only encrypted by a non-exportable
 * Android Keystore key. Without this phone's Keystore the database file cannot be opened.
 */
class DatabaseKey(private val context: Context, private val alias: String = DEFAULT_ALIAS) {

    private val file: File get() = File(context.noBackupFilesDir, "$alias.key")

    @Synchronized
    fun passphrase(): ByteArray {
        if (file.exists()) return decrypt(file.readBytes())
        val secret = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeBytes(encrypt(secret))
        check(tmp.renameTo(file)) { "could not store database key" }
        return secret
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply { init(spec) }.generateKey()
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, keystoreKey()) }
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plain)
    }

    private fun decrypt(stored: ByteArray): ByteArray {
        val ivSize = stored[0].toInt()
        val iv = stored.copyOfRange(1, 1 + ivSize)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        }
        return cipher.doFinal(stored.copyOfRange(1 + ivSize, stored.size))
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val DEFAULT_ALIAS = "sarejam_db"
    }
}

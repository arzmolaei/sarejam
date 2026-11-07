package ir.sarejam.core.database

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

class DatabaseKeyException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

/** A random database key is persisted only inside an authenticated, versioned Keystore envelope. */
@Singleton
class DatabaseKeyManager internal constructor(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val keyAlias: String,
    private val preferenceStoreIdentity: String,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, PREFERENCES_NAME, KEY_ALIAS)

    internal constructor(context: Context, preferencesName: String, keyAlias: String) : this(
        context.applicationContext,
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE),
        keyAlias,
        "${context.applicationContext.packageName}:$preferencesName",
    )

    /** Caller owns and must erase the returned key after passing it to the database factory. */
    fun getOrCreateKey(): ByteArray = synchronized(creationLock) {
        try {
            if (preferenceStoreIdentity in uncertainPersistenceStores) {
                throw DatabaseKeyException("The database key store is unavailable after a persistence failure.")
            }
            if (preferences.contains(ENVELOPE)) {
                unwrap(preferences.getString(ENVELOPE, null)
                    ?: throw DatabaseKeyException("The database key envelope is missing."))
            } else {
                // A missing wrapper must never cause an existing database to be silently abandoned.
                if (preferences.all.isNotEmpty() || context.getDatabasePath(SarejamDatabase.DEFAULT_NAME).exists()) {
                    throw DatabaseKeyException("The existing database key envelope is missing.")
                }
                create()
            }
        } catch (failure: DatabaseKeyException) {
            throw failure
        } catch (failure: Exception) {
            // No key material, message body or financial data is included in this message.
            throw DatabaseKeyException("The database key could not be securely accessed.", failure)
        }
    }

    private fun unwrap(encoded: String): ByteArray {
        val envelope = JSONObject(encoded)
        if (envelope.opt("version") != ENVELOPE_VERSION) {
            throw DatabaseKeyException("The database key envelope version is unsupported.")
        }
        val iv = decode(envelope.getString("iv"))
        val ciphertext = decode(envelope.getString("ciphertext"))
        if (iv.size != IV_SIZE || ciphertext.size != KEY_SIZE + TAG_SIZE) {
            throw DatabaseKeyException("The database key envelope is invalid.")
        }
        val wrappingKey = loadKeyStore().getKey(keyAlias, null) as? SecretKey
            ?: throw DatabaseKeyException("The database wrapping key is missing.")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(TAG_SIZE * 8, iv))
        cipher.updateAAD(associatedData)
        val plaintext = cipher.doFinal(ciphertext)
        if (plaintext.size != KEY_SIZE) {
            plaintext.fill(0)
            throw DatabaseKeyException("The unwrapped database key is invalid.")
        }
        return plaintext
    }

    private fun create(): ByteArray {
        val random = SecureRandom()
        val databaseKey = ByteArray(KEY_SIZE).also(random::nextBytes)
        try {
            val keyStore = loadKeyStore()
            val wrappingKey = if (keyStore.containsAlias(keyAlias)) {
                keyStore.getKey(keyAlias, null) as? SecretKey
                    ?: throw DatabaseKeyException("The database wrapping key is invalid.")
            } else {
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
                    init(
                        KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                            .setKeySize(256)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(true)
                            .setUserAuthenticationRequired(false)
                            .build(),
                    )
                    generateKey()
                }
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, random)
            cipher.updateAAD(associatedData)
            val ciphertext = cipher.doFinal(databaseKey)
            val envelope = JSONObject()
                .put("version", ENVELOPE_VERSION)
                .put("iv", encode(cipher.iv))
                .put("ciphertext", encode(ciphertext))
                .toString()
            // SharedPreferences updates its process cache before attempting disk I/O. Until a
            // successful commit, no instance may trust that cache; a false/throwing commit leaves
            // this store blocked until process restart. Recommitting the same value is insufficient.
            uncertainPersistenceStores.add(preferenceStoreIdentity)
            if (!preferences.edit().putString(ENVELOPE, envelope).commit()) {
                throw DatabaseKeyException("The database key envelope could not be persisted.")
            }
            uncertainPersistenceStores.remove(preferenceStoreIdentity)
            return databaseKey
        } catch (failure: Exception) {
            databaseKey.fill(0)
            throw failure
        }
    }

    private fun loadKeyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val PREFERENCES_NAME = "sarejam.database.key"
        const val KEY_ALIAS = "ir.sarejam.database.wrapping.v1"
        const val ENVELOPE = "envelope"
        const val ENVELOPE_VERSION = 1
        const val KEY_SIZE = 32
        const val IV_SIZE = 12
        const val TAG_SIZE = 16
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        val associatedData = "ir.sarejam.database-key.v1".toByteArray(Charsets.UTF_8)
        // Serialize all instances, including simultaneous Hilt and background-worker access.
        val creationLock = Any()
        // Store identities contain no key material; all access is under creationLock.
        val uncertainPersistenceStores = mutableSetOf<String>()
    }
}

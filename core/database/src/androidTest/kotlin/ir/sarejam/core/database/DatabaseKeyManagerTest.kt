package ir.sarejam.core.database

import android.content.Context
import android.security.keystore.KeyInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.crypto.SecretKeyFactory
import javax.crypto.SecretKey
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseKeyManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencesName = "key-test-${UUID.randomUUID()}"
    private val alias = "key-test-${UUID.randomUUID()}"
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private fun manager() = DatabaseKeyManager(context, preferencesName, alias)

    @After
    fun cleanUp() {
        preferences.edit().clear().commit()
        context.deleteSharedPreferences(preferencesName)
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test
    fun keySurvivesManagerRecreationAndIsStoredOnlyInAuthenticatedEnvelope() {
        val first = manager().getOrCreateKey()
        val second = manager().getOrCreateKey()
        try {
            assertEquals(32, first.size)
            assertTrue(first.contentEquals(second))
            val envelope = org.json.JSONObject(preferences.getString("envelope", null)!!)
            assertEquals(1, envelope.getInt("version"))
            assertEquals(12, android.util.Base64.decode(envelope.getString("iv"), android.util.Base64.NO_WRAP).size)
            val ciphertext = android.util.Base64.decode(envelope.getString("ciphertext"), android.util.Base64.NO_WRAP)
            assertEquals(48, ciphertext.size)
            assertFalse(ciphertext.contentEquals(first))
            val wrappingKey = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(alias, null) as SecretKey
            val info = SecretKeyFactory.getInstance(wrappingKey.algorithm, "AndroidKeyStore").getKeySpec(wrappingKey, KeyInfo::class.java) as KeyInfo
            assertFalse(info.isUserAuthenticationRequired)
        } finally {
            first.fill(0)
            second.fill(0)
        }
    }

    @Test
    fun concurrentManagerInstancesCreateExactlyOneDatabaseKey() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            val keys = executor.invokeAll((1..8).map { Callable { manager().getOrCreateKey() } }).map { it.get() }
            try { assertTrue(keys.all { it.contentEquals(keys.first()) }) } finally { keys.forEach { it.fill(0) } }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun corruptedAuthenticatedCiphertextFailsWithoutReplacingTheEnvelope() {
        manager().getOrCreateKey().fill(0)
        val envelope = org.json.JSONObject(preferences.getString("envelope", null)!!)
        val encrypted = android.util.Base64.decode(envelope.getString("ciphertext"), android.util.Base64.NO_WRAP)
        encrypted[0] = (encrypted[0].toInt() xor 1).toByte()
        envelope.put("ciphertext", android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
        val corrupted = envelope.toString()
        assertTrue(preferences.edit().putString("envelope", corrupted).commit())
        expectFailure { manager().getOrCreateKey() }
        assertEquals(corrupted, preferences.getString("envelope", null))
    }

    @Test
    fun malformedOrUnsupportedEnvelopeFailsWithoutRegeneration() {
        for (malformed in listOf("not-json", "{}", "{\"version\":2,\"iv\":\"\",\"ciphertext\":\"\"}", "{\"version\":1,\"iv\":\"invalid\",\"ciphertext\":\"invalid\"}")) {
            assertTrue(preferences.edit().putString("envelope", malformed).commit())
            expectFailure { manager().getOrCreateKey() }
            assertEquals(malformed, preferences.getString("envelope", null))
            assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(alias))
        }
    }

    @Test
    fun missingWrappingAliasFailsWithoutRegeneratingAnExistingKey() {
        manager().getOrCreateKey().fill(0)
        val before = preferences.getString("envelope", null)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(alias)
        expectFailure { manager().getOrCreateKey() }
        assertEquals(before, preferences.getString("envelope", null))
        assertFalse(keyStore.containsAlias(alias))
    }

    @Test
    fun failedEnvelopeCommitBlocksOriginalAndFreshManagersDespiteUpdatedCache() {
        val storage = CacheUpdatingFailedCommitPreferences()
        val storeIdentity = "${context.packageName}:$preferencesName"
        val original = DatabaseKeyManager(context, storage, alias, storeIdentity)
        expectFailure { original.getOrCreateKey().fill(0) }
        assertTrue("A false commit can still populate the process cache", storage.contains("envelope"))
        assertEquals(1, storage.commitAttempts)

        // A cached decryptable value does not prove the envelope survived a disk write.
        expectFailure { original.getOrCreateKey().fill(0) }
        val fresh = DatabaseKeyManager(context, storage, alias, storeIdentity)
        expectFailure { fresh.getOrCreateKey().fill(0) }
        assertEquals("Recommitting a cached value is not proof of persistence", 1, storage.commitAttempts)
    }

    @Test
    fun missingEnvelopeWithExistingDefaultDatabaseFailsWithoutCreatingAReplacementKey() = runBlocking {
        val path = context.getDatabasePath(SarejamDatabase.DEFAULT_NAME)
        assertFalse("Test target must start without a default database", path.exists())
        val databaseKey = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
        try {
            val initial = SarejamDatabase.open(context, databaseKey)
            try {
                initial.accountDao().insert(AccountEntity(name = "Synthetic guarded account", kind = AccountKind.CASH, space = AccountSpace.PERSONAL, createdAt = 1, updatedAt = 1))
            } finally { initial.close() }
            assertTrue(path.exists())
            expectFailure { manager().getOrCreateKey().fill(0) }
            assertFalse(preferences.contains("envelope"))
            assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(alias))
            val reopened = SarejamDatabase.open(context, databaseKey)
            try {
                assertEquals("Synthetic guarded account", reopened.accountDao().listAll().single().name)
            } finally { reopened.close() }
        } finally {
            databaseKey.fill(0)
            context.deleteDatabase(SarejamDatabase.DEFAULT_NAME)
        }
    }
}

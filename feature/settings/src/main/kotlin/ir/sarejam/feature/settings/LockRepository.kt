package ir.sarejam.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lockStore: DataStore<Preferences> by preferencesDataStore(name = "lock")

/** Lock settings on the phone only. Default: lock off (docs/PRODUCT.md). */
class LockRepository(context: Context) {
    private val store = context.applicationContext.lockStore

    val config: Flow<LockConfig> = store.data.map { p ->
        LockConfig(
            enabled = p[ENABLED] ?: false,
            pinHash = p[PIN_HASH],
            biometric = p[BIOMETRIC] ?: false,
            autoLockSeconds = p[AUTO_LOCK] ?: 60,
        )
    }

    suspend fun current(): LockConfig = config.first()

    suspend fun enable(pin: String, biometric: Boolean) {
        val hash = PinHasher.hash(pin)
        store.edit { it[PIN_HASH] = hash; it[BIOMETRIC] = biometric; it[ENABLED] = true }
    }

    suspend fun disable() {
        store.edit { it.remove(PIN_HASH); it[BIOMETRIC] = false; it[ENABLED] = false }
    }

    suspend fun setAutoLock(seconds: Int) {
        store.edit { it[AUTO_LOCK] = seconds }
    }

    suspend fun setBiometric(on: Boolean) {
        store.edit { it[BIOMETRIC] = on }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val AUTO_LOCK = intPreferencesKey("auto_lock_seconds")
    }
}

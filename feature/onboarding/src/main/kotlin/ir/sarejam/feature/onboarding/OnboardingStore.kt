package ir.sarejam.feature.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingStore by preferencesDataStore(name = "onboarding")

class OnboardingStore(context: Context) {
    private val store = context.applicationContext.onboardingStore
    private val done = booleanPreferencesKey("done")

    val isDone = store.data.map { it[done] ?: false }
    suspend fun done(): Boolean = isDone.first()
    suspend fun markDone() { store.edit { it[done] = true } }
    suspend fun reset() { store.edit { it.remove(done) } }
}

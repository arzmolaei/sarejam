package ir.sarejam.core.database

import android.content.SharedPreferences

/** Models SharedPreferences' cache update before a failed disk write; encryption stays real. */
internal class CacheUpdatingFailedCommitPreferences : SharedPreferences {
    private val cache = mutableMapOf<String, Any>()
    var commitAttempts = 0
        private set

    override fun contains(key: String?) = cache.containsKey(key)
    override fun getAll(): Map<String, *> = cache.toMap()
    override fun getString(key: String?, defValue: String?) = cache[key] as String? ?: defValue
    override fun getInt(key: String?, defValue: Int) = cache[key] as Int? ?: defValue
    override fun getLong(key: String?, defValue: Long) = cache[key] as Long? ?: defValue
    override fun getFloat(key: String?, defValue: Float) = cache[key] as Float? ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean) = cache[key] as Boolean? ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (cache[key] as Set<String>?)?.toMutableSet() ?: defValues
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private var clear = false
        private fun put(key: String?, value: Any?): SharedPreferences.Editor = apply { pending[requireNotNull(key)] = value }
        override fun putString(key: String?, value: String?) = put(key, value)
        override fun putInt(key: String?, value: Int) = put(key, value)
        override fun putLong(key: String?, value: Long) = put(key, value)
        override fun putFloat(key: String?, value: Float) = put(key, value)
        override fun putBoolean(key: String?, value: Boolean) = put(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.toSet())
        override fun remove(key: String?) = put(key, null)
        override fun clear(): SharedPreferences.Editor = apply { clear = true }
        override fun commit(): Boolean {
            commitAttempts++
            if (clear) cache.clear()
            pending.forEach { (key, value) -> if (value == null) cache.remove(key) else cache[key] = value }
            return false
        }
        override fun apply() { commit() }
    }
}

package io.nekohasekai.sagernet.database.preference

/**
 * Pure Kotlin replacement for androidx.preference.PreferenceDataStore so the
 * data layer has zero dependency on legacy AndroidX View Preferences.
 */
abstract class PreferenceDataStore {
    open fun putString(key: String, value: String?) = Unit
    open fun putStringSet(key: String, values: MutableSet<String>?) = Unit
    open fun putInt(key: String, value: Int) = Unit
    open fun putLong(key: String, value: Long) = Unit
    open fun putFloat(key: String, value: Float) = Unit
    open fun putBoolean(key: String, value: Boolean) = Unit

    open fun getString(key: String, defValue: String?): String? = defValue
    open fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    open fun getInt(key: String, defValue: Int): Int = defValue
    open fun getLong(key: String, defValue: Long): Long = defValue
    open fun getFloat(key: String, defValue: Float): Float = defValue
    open fun getBoolean(key: String, defValue: Boolean): Boolean = defValue
}

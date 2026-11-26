package io.nekohasekai.sagernet.database.preference

import io.nekohasekai.sagernet.database.preference.PreferenceDataStore

interface OnPreferenceDataStoreChangeListener {
    fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String)
}

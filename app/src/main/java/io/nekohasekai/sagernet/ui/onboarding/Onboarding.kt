package io.nekohasekai.sagernet.ui.onboarding

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object Onboarding {
    private const val PREF_FILE = "re_nekobox_onboarding"
    private const val KEY_DONE = "done"

    @Volatile
    private var appContext: Context? = null

    private val _done = MutableStateFlow(true)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        _done.value =
            app
                .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .getBoolean(KEY_DONE, false)
    }

    fun complete() {
        _done.value = true
        appContext
            ?.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_DONE, true)
            ?.apply()
    }
}

package io.nekohasekai.sagernet.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.app

/**
 * Theme constants & night-mode coordinator.
 * All 22 color palettes + Dynamic Color (Material You) are rendered purely in
 * Jetpack Compose via [io.nekohasekai.sagernet.ui.theme.ReNekoBoxTheme].
 */
object Theme {

    const val DYNAMIC = 0
    const val RED = 1
    const val PINK_SSR = 2
    const val PINK = 3
    const val PURPLE = 4
    const val DEEP_PURPLE = 5
    const val INDIGO = 6
    const val BLUE = 7
    const val LIGHT_BLUE = 8
    const val CYAN = 9
    const val TEAL = 10
    const val GREEN = 11
    const val LIGHT_GREEN = 12
    const val LIME = 13
    const val YELLOW = 14
    const val AMBER = 15
    const val ORANGE = 16
    const val DEEP_ORANGE = 17
    const val BROWN = 18
    const val GREY = 19
    const val BLUE_GREY = 20
    const val BLACK = 21
    const val VERDANT_MINT = 22

    fun apply(context: Context) {
        context.setTheme(R.style.Theme_ReNekoBox)
    }

    fun applyDialog(context: Context) {
        context.setTheme(R.style.Theme_ReNekoBox_Dialog)
    }

    fun getTheme(): Int = R.style.Theme_ReNekoBox
    fun getTheme(theme: Int): Int = R.style.Theme_ReNekoBox
    fun getDialogTheme(): Int = R.style.Theme_ReNekoBox_Dialog
    fun getDialogTheme(theme: Int): Int = R.style.Theme_ReNekoBox_Dialog

    var currentNightMode = -1
    fun getNightMode(): Int {
        if (currentNightMode == -1) {
            currentNightMode = DataStore.nightTheme
        }
        return getNightMode(currentNightMode)
    }

    fun getNightMode(mode: Int): Int = when (mode) {
        0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        1 -> AppCompatDelegate.MODE_NIGHT_YES
        2 -> AppCompatDelegate.MODE_NIGHT_NO
        else -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
    }

    fun usingNightMode(): Boolean = when (DataStore.nightTheme) {
        1 -> true
        2 -> false
        else -> (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun applyNightTheme() {
        AppCompatDelegate.setDefaultNightMode(getNightMode())
    }
}
package io.nekohasekai.sagernet.ktx

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import io.nekohasekai.sagernet.R

fun Context.alert(text: String): AlertDialog {
    return AlertDialog.Builder(this).setTitle(R.string.error_title)
        .setMessage(text)
        .setPositiveButton(android.R.string.ok, null)
        .create()
}

fun AlertDialog.tryToShow() {
    try {
        val activity = context as Activity
        if (!activity.isFinishing) {
            show()
        }
    } catch (e: Exception) {
        Logs.e(e)
    }
}

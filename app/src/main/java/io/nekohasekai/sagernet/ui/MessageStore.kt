package io.nekohasekai.sagernet.ui

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.runOnMainDispatcher
import java.lang.ref.WeakReference

object MessageStore {
    private var currentActivity: WeakReference<Activity>? = null

    fun setCurrentActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    fun showMessage(message: String) {
        val activity = currentActivity?.get() ?: return
        try {
            if (activity is ThemedActivity) {
                activity.showComposeSnackbar(message)
            } else {
                runOnMainDispatcher {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Logs.w("Failed to show message", e)
        }
    }

    fun showMessage(activity: Activity, @StringRes resId: Int) {
        showMessage(activity.getString(resId))
    }

    fun showMessage(activity: Activity, message: String) {
        showMessage(message)
    }

    fun showMessage(activity: Activity, @StringRes resId: Int, vararg formatArgs: Any) {
        showMessage(activity.getString(resId, *formatArgs))
    }
}

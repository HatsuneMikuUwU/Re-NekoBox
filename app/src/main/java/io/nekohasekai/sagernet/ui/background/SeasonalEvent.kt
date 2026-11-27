package io.nekohasekai.sagernet.ui.background

import androidx.annotation.StringRes
import io.nekohasekai.sagernet.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class SeasonalEvent(
    @StringRes val labelRes: Int,
    val background: BackgroundType,
    private val startMonth: Int,
    private val startDay: Int,
    val endMonth: Int,
    val endDay: Int,
) {
    HALLOWEEN(R.string.ui_season_halloween, BackgroundType.HALLOWEEN, 10, 24, 11, 1),
    NEW_YEAR(R.string.ui_season_new_year, BackgroundType.FIREWORKS, 12, 31, 1, 1),
    WINTER_HOLIDAYS(R.string.ui_season_winter_holidays, BackgroundType.SNOW, 12, 20, 1, 7),
    ;

    val endLabel: String
        get() {
            val locale = Locale.getDefault()
            val cal = Calendar.getInstance().apply {
                set(Calendar.MONTH, endMonth - 1)
                set(Calendar.DAY_OF_MONTH, endDay)
            }
            val pattern = if (locale.language == Locale.CHINESE.language) "M月d日" else "d MMMM"
            return SimpleDateFormat(pattern, locale).format(cal.time)
        }

    private fun isRunningOn(key: Int): Boolean {
        val start = startMonth * 100 + startDay
        val end = endMonth * 100 + endDay
        return if (start <= end) key in start..end else key >= start || key <= end
    }

    companion object {
        fun on(calendar: Calendar = Calendar.getInstance()): SeasonalEvent? {
            val key = (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH)
            return entries.firstOrNull { it.isRunningOn(key) }
        }
    }
}

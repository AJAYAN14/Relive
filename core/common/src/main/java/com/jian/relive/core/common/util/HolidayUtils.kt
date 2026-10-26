package com.jian.relive.core.common.util

import android.icu.util.ChineseCalendar
import java.util.Calendar
import java.util.Date

/**
 * 节日与农历特殊日期计算工具。
 */
object HolidayUtils {

    /**
     * 根据公历日期计算对应的节日标准标识键。
     *
     * @param date 待检测的目标日期。
     * @return 匹配到的节日标准标识键，无节日时返回 null。
     */
    fun getHolidayKey(date: Date): String? {
        val cal = Calendar.getInstance()
        cal.time = date
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        if (month == 1 && day == 1) return "New Year's Day"
        if (month == 2 && day == 14) return "Valentine's Day"
        if (month == 5 && day in 1..5) return "Labor Day Holiday"
        if (month == 6 && day == 1) return "Children's Day"
        if (month == 10 && day in 1..7) return "National Day Holiday"

        val chineseCal = ChineseCalendar()
        chineseCal.time = date
        val isLeap = chineseCal.get(ChineseCalendar.IS_LEAP_MONTH) == 1

        if (!isLeap) {
            val lunarMonth = chineseCal.get(ChineseCalendar.MONTH) + 1
            val lunarDay = chineseCal.get(ChineseCalendar.DAY_OF_MONTH)

            if (lunarMonth == 1 && lunarDay in 1..7) return "Spring Festival"
            if (lunarMonth == 5 && lunarDay == 5) return "Dragon Boat Festival"
            if (lunarMonth == 8 && lunarDay == 15) return "Mid-Autumn Festival"
        }

        return null
    }
}

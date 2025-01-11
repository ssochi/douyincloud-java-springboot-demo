package com.bytedance.douyinclouddemo.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    /**
     * 判断日期是否在当前周
     * @param date 待判断的日期，如果为null返回true
     * @return 是否在当前周
     */
    public static boolean isCurrentWeek(Date date) {
        if (date == null) {
            return true;
        }

        LocalDate targetDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate now = LocalDate.now();

        // 使用WeekFields获取周信息
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int targetWeek = targetDate.get(weekFields.weekOfWeekBasedYear());
        int targetYear = targetDate.get(weekFields.weekBasedYear());
        int currentWeek = now.get(weekFields.weekOfWeekBasedYear());
        int currentYear = now.get(weekFields.weekBasedYear());

        return targetWeek == currentWeek && targetYear == currentYear;
    }

    /**
     * 判断日期是否在当前月
     * @param date 待判断的日期，如果为null返回true
     * @return 是否在当前月
     */
    public static boolean isCurrentMonth(Date date) {
        if (date == null) {
            return true;
        }

        LocalDate targetDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate now = LocalDate.now();

        return targetDate.getMonth() == now.getMonth()
                && targetDate.getYear() == now.getYear();
    }
}

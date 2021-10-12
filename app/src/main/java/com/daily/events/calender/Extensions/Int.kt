package com.daily.events.calender.Extensions

import com.daily.events.calender.helpers.MONTH
import com.daily.events.calender.helpers.WEEK
import com.daily.events.calender.helpers.YEAR

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0

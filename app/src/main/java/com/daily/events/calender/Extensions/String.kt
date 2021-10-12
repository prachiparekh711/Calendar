package com.daily.events.calender.Extensions

fun String.getMonthCode() = if (length == 8) substring(0, 6) else ""

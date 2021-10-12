package com.daily.events.calender.Extensions

import com.daily.events.calender.Model.Event
import com.daily.events.calender.helpers.Formatter

fun Long.isTsOnProperDay(event: Event): Boolean {
    val dateTime = Formatter.getDateTimeFromTS(this)
    val power = Math.pow(2.0, (dateTime.dayOfWeek - 1).toDouble()).toInt()
    return event.repeatRule and power != 0
}

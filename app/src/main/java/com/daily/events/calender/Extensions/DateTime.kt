package com.daily.events.calender.Extensions

import org.joda.time.DateTime

fun DateTime.seconds() = millis / 1000L

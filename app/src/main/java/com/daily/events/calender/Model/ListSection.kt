package com.daily.events.calender.models

import com.daily.events.calender.Model.ListItem

data class ListSection(
    val title: String,
    val code: String,
    val isToday: Boolean,
    val isPastSection: Boolean
) : ListItem()

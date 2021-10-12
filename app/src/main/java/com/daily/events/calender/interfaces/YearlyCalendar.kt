package com.daily.events.calender.interfaces

import android.util.SparseArray
import com.daily.events.calender.Model.DayYearly
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}

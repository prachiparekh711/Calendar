package com.daily.events.calender.Adapter

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.daily.events.calender.Fragment.Home.DayFragment
import com.daily.events.calender.helpers.WEEK_START_TIMESTAMP
import com.daily.events.calender.interfaces.WeekFragmentListener
import com.simplemobiletools.commons.helpers.DAY_SECONDS

class MyDayPagerAdapter(
    fm: FragmentManager,
    private val mWeekTimestamps: List<Long>,
    private val mListener: WeekFragmentListener
) : FragmentStatePagerAdapter(fm) {
    private val mFragments = SparseArray<DayFragment>()

    override fun getCount() = mWeekTimestamps.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val weekTimestamp = mWeekTimestamps[position]
        bundle.putLong(WEEK_START_TIMESTAMP, weekTimestamp)

        val fragment = DayFragment()
        fragment.arguments = bundle
        fragment.listener = mListener

        mFragments.put(position, fragment)
        return fragment
    }

    fun updateScrollY(pos: Int, y: Int) {
        mFragments[pos - 1]?.updateScrollY(y)
        mFragments[pos + 1]?.updateScrollY(y)
    }

    fun updateCalendars(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.updateCalendar()
        }
    }

    fun updateNotVisibleScaleLevel(pos: Int) {
        mFragments[pos - 1]?.updateNotVisibleViewScaleLevel()
        mFragments[pos + 1]?.updateNotVisibleViewScaleLevel()
    }

    fun updateVisibleDaysCount(pos: Int, count: Int, currentWeekTimestamp: Long) {
        mFragments[pos - 1]?.updateWeekStartTimestamp(currentWeekTimestamp - count * DAY_SECONDS)
        mFragments[pos + 1]?.updateWeekStartTimestamp(currentWeekTimestamp + count * DAY_SECONDS)

        for (i in -1..1) {
            mFragments[pos + i]?.updateVisibleDaysCount(count)
        }
    }

    fun togglePrintMode(pos: Int) {
        mFragments[pos].togglePrintMode()
    }
}

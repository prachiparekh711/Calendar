package com.daily.events.calender.Fragment.Home

import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.daily.events.calender.Activity.MainActivity
import com.daily.events.calender.Extensions.config
import com.daily.events.calender.Extensions.seconds
import com.daily.events.calender.Fragment.DayFragmentsHolder
import com.daily.events.calender.Fragment.MonthFragmentsHolder
import com.daily.events.calender.Fragment.WeekFragmentsHolder
import com.daily.events.calender.Fragment.YearFragmentsHolder
import com.daily.events.calender.R
import com.daily.events.calender.databinding.FragmentHomeBinding
import com.daily.events.calender.helpers.*
import com.daily.events.calender.helpers.Formatter
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment(), View.OnClickListener {


    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = requireActivity()

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        fragmentHomeBinding?.yearIV?.setOnClickListener(this)
        fragmentHomeBinding?.monthIV?.setOnClickListener(this)
        fragmentHomeBinding?.weekIV?.setOnClickListener(this)
        fragmentHomeBinding?.dayIV?.setOnClickListener(this)
        cfm = childFragmentManager
        if (requireActivity().config.storedView == YEARLY_VIEW) {
            yearChanges()

        } else if (requireActivity().config.storedView == MONTHLY_VIEW) {
            monthChanges()
        } else if (requireActivity().config.storedView == WEEKLY_VIEW) {
            weekChanges()
        } else {
            dayChanges()
        }
        updateViewPager()

        return fragmentHomeBinding?.root
    }

    companion object {
        var fragmentHomeBinding: FragmentHomeBinding? = null
        var mActivity: Activity? = null
        var cfm: FragmentManager? = null

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        fun weekChanges() {
            mActivity?.let {
                fragmentHomeBinding?.yearIV?.setColorFilter(
                    ContextCompat.getColor(
                        it,
                        R.color.grey
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                fragmentHomeBinding?.monthIV?.setColorFilter(
                    ContextCompat.getColor(
                        it,
                        R.color.grey
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                fragmentHomeBinding?.weekIV?.setColorFilter(
                    ContextCompat.getColor(
                        it,
                        R.color.theme_color
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                fragmentHomeBinding?.dayIV?.setColorFilter(
                    ContextCompat.getColor(
                        it,
                        R.color.grey
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                mActivity?.config?.storedView = WEEKLY_VIEW
                updateViewPager()
            }
        }

        fun updateViewPager(dayCode: String? = Formatter.getTodayCode()) {

            val fragment = getFragmentsHolder()
            MainActivity.mainBinding?.today?.setOnClickListener {
                if (MainActivity.mainBinding!!.navigationDrawer.isDrawerOpen)
                    MainActivity.mainBinding!!.navigationDrawer.closeDrawer()
                fragment.goToToday()
            }
            val bundle = Bundle()

            if (mActivity?.config?.storedView == MONTHLY_VIEW ||
                mActivity?.config?.storedView == MONTHLY_DAILY_VIEW
            ) {
                bundle.putString(DAY_CODE, dayCode)
            } else if (mActivity?.config?.storedView == WEEKLY_VIEW) {
                bundle.putString(WEEK_START_DATE_TIME, getThisWeekDateTime())
            } else if (mActivity?.config?.storedView == DAILY_VIEW) {
                bundle.putString(DAY_CODE, dayCode)
                bundle.putString(WEEK_START_DATE_TIME, getThisWeekDateTime())
            }

            fragment.arguments = bundle
            cfm?.beginTransaction()?.replace(R.id.container1, fragment)?.commitNow()
        }

        private fun getThisWeekDateTime(): String {
            val currentOffsetHours = TimeZone.getDefault().rawOffset / 1000 / 60 / 60

            // not great, not terrible
            val useHours = if (currentOffsetHours >= 10) 8 else 12
            var thisweek =
                DateTime().withZone(DateTimeZone.UTC).withDayOfWeek(1).withHourOfDay(useHours)
                    .minusDays(if (mActivity?.config?.isSundayFirst == true) 1 else 0)
            if (DateTime().minusDays(7).seconds() > thisweek.seconds()) {
                thisweek = thisweek.plusDays(7)
            }
            return thisweek.toString()
        }

        private fun getFragmentsHolder() = when (mActivity?.config?.storedView) {
            DAILY_VIEW -> DayFragmentsHolder()
            MONTHLY_VIEW -> MonthFragmentsHolder()
            WEEKLY_VIEW -> WeekFragmentsHolder()
            YEARLY_VIEW -> YearFragmentsHolder()
            else -> MonthFragmentsHolder()
        }

    }

    fun yearChanges() {
        activity?.let {
            fragmentHomeBinding?.yearIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.theme_color
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.monthIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.weekIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.dayIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )

        }
    }

    fun monthChanges() {
        activity?.let {
            fragmentHomeBinding?.yearIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.monthIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.theme_color
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.weekIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.dayIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )

        }
    }


    fun dayChanges() {
        activity?.let {
            fragmentHomeBinding?.yearIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.monthIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.weekIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.grey
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fragmentHomeBinding?.dayIV?.setColorFilter(
                ContextCompat.getColor(
                    it,
                    R.color.theme_color
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            requireActivity().config.storedView = DAILY_VIEW
            updateViewPager()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.yearIV -> {
                yearChanges()
                requireActivity().config.storedView = YEARLY_VIEW
                updateViewPager()
            }
            R.id.monthIV -> {
                monthChanges()
                requireActivity().config.storedView = MONTHLY_VIEW
                updateViewPager()
            }
            R.id.weekIV -> {
                AddWeekTask().execute()
//                weekChanges()
//                requireActivity().config.storedView = WEEKLY_VIEW
//                updateViewPager()
            }
            R.id.dayIV -> {
                dayChanges()
                requireActivity().config.storedView = DAILY_VIEW
                updateViewPager()
            }
        }
    }

    class AddWeekTask : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void?): String {
            mActivity?.runOnUiThread {
                Log.e("week", "doInBackground")
                weekChanges()
                updateViewPager()
            }
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

        }
    }


}
package com.daily.events.calender.Fragment.Home

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.daily.events.calender.Activity.MainActivity
import com.daily.events.calender.Extensions.config
import com.daily.events.calender.Extensions.getViewBitmap
import com.daily.events.calender.Extensions.printBitmap
import com.daily.events.calender.Model.DayYearly
import com.daily.events.calender.R
import com.daily.events.calender.databinding.FragmentYearBinding
import com.daily.events.calender.helpers.MONTHLY_VIEW
import com.daily.events.calender.helpers.YEAR_LABEL
import com.daily.events.calender.helpers.YearlyCalendarImpl
import com.daily.events.calender.interfaces.YearlyCalendar
import com.daily.events.calender.views.SmallMonthView
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.fragment_year.view.*
import org.joda.time.DateTime
import java.util.*


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class YearFragment : Fragment(), YearlyCalendar {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    var fragmentYearBinding: FragmentYearBinding? = null
    private var mYear = 0
    private var mCalendar: YearlyCalendarImpl? = null
    lateinit var mView: View
    private var lastHash = 0
    private var isPrintVersion = false
    private var mSundayFirst = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_year, container, false)
        fragmentYearBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_year, container, false)
        mYear = requireArguments().getInt(YEAR_LABEL)
        requireContext().updateTextColors(mView.calendar_holder)
        setupMonths()

        mCalendar = YearlyCalendarImpl(this, requireContext(), mYear)
        return mView
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            YearFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun setupMonths() {
//        Log.e("year", mYear.toString())
        val dateTime = DateTime().withDate(mYear, 2, 1).withHourOfDay(12)
        val days = dateTime.dayOfMonth().maximumValue
        mView.month_2.setDays(days)

        val now = DateTime()

        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(
                resources.getIdentifier(
                    "month_$i",
                    "id",
                    requireContext().packageName
                )
            )
            var dayOfWeek = dateTime.withMonthOfYear(i).dayOfWeek().get()
            dayOfWeek--

            val monthLabel = mView.findViewById<TextView>(
                resources.getIdentifier(
                    "month_${i}_label",
                    "id",
                    requireContext().packageName
                )
            )

            monthLabel.setTextColor(resources.getColor(R.color.black))
            monthView.firstDay = dayOfWeek
            monthView.setOnClickListener {
                requireActivity().config.storedView = MONTHLY_VIEW
                (activity as MainActivity).openMonthFromYearly(DateTime().withDate(mYear, i, 1))
            }
        }

        if (!isPrintVersion) {
            markCurrentMonth(now)
        }
    }

    fun updateCalendar() {
        mCalendar?.getEvents(mYear)
    }

    private fun markCurrentMonth(now: DateTime) {
        if (now.year == mYear) {
            val monthLabel = mView.findViewById<TextView>(
                resources.getIdentifier(
                    "month_${now.monthOfYear}_label",
                    "id",
                    requireContext().packageName
                )
            )
            monthLabel.setTextColor(requireContext().getAdjustedPrimaryColor())

            val monthView = mView.findViewById<SmallMonthView>(
                resources.getIdentifier(
                    "month_${now.monthOfYear}",
                    "id",
                    requireContext().packageName
                )
            )
            monthView.todaysId = now.dayOfMonth
        }
    }

    override fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int) {
        if (!isAdded)
            return

        if (hashCode == lastHash) {
            return
        }

        lastHash = hashCode
        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(
                resources.getIdentifier(
                    "month_$i",
                    "id",
                    requireContext().packageName
                )
            )
            monthView.setEvents(events.get(i))
        }
    }

    fun printCurrentView() {
        isPrintVersion = true
        setupMonths()
        toggleSmallMonthPrintModes()

        requireContext().printBitmap(mView.calendar_holder.getViewBitmap())

        isPrintVersion = false
        setupMonths()
        toggleSmallMonthPrintModes()
    }

    private fun toggleSmallMonthPrintModes() {
        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(
                resources.getIdentifier(
                    "month_$i",
                    "id",
                    requireContext().packageName
                )
            )
            monthView.togglePrintMode()
        }
    }

    override fun onResume() {
        super.onResume()
        val sundayFirst = requireContext().config.isSundayFirst
        if (sundayFirst != mSundayFirst) {
            mSundayFirst = sundayFirst
            setupMonths()
        }
        updateCalendar()
    }

    override fun onPause() {
        super.onPause()
        mSundayFirst = requireContext().config.isSundayFirst
    }
}
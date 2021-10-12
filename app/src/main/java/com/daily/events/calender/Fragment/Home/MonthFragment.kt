package com.daily.events.calender.Fragment.Home

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import com.daily.events.calender.Activity.MainActivity
import com.daily.events.calender.Activity.SimpleActivity
import com.daily.events.calender.Adapter.EventListAdapter
import com.daily.events.calender.Extensions.*
import com.daily.events.calender.Model.DayMonthly
import com.daily.events.calender.Model.Event
import com.daily.events.calender.R
import com.daily.events.calender.helpers.Config
import com.daily.events.calender.helpers.DAY_CODE
import com.daily.events.calender.helpers.Formatter
import com.daily.events.calender.helpers.MonthlyCalendarImpl
import com.daily.events.calender.interfaces.MonthlyCalendar
import com.daily.events.calender.interfaces.NavigationListener
import com.daily.events.calender.models.ListEvent
import com.daily.events.calender.views.MonthViewWrapper
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import kotlinx.android.synthetic.main.fragment_month.view.*
import kotlinx.android.synthetic.main.layout_monthview_event.*
import kotlinx.android.synthetic.main.layout_monthview_event.view.*
import org.joda.time.DateTime


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MonthFragment : Fragment(), MonthlyCalendar, RefreshRecyclerViewListener,
    View.OnTouchListener {

    private var mSundayFirst = false
    private var mShowWeekNumbers = false
    private var mDayCode = ""
    private var mPackageName = ""
    private var mLastHash = 0L
    private var mCalendar: MonthlyCalendarImpl? = null

    var listener: NavigationListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    lateinit var mMainRL: RelativeLayout
    lateinit var imgAddEvent: AppCompatImageView
    lateinit var mMonthViewWaraper: MonthViewWrapper
    lateinit var mConfig: Config

    private var mSelectedDayCode = ""

    private var mListEvents = ArrayList<Event>()

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_month, container, false)

        mRes = resources
        mPackageName = requireActivity().packageName
        mHolder = view.month_calendar_holder
        mMonthViewWaraper = view.month_view_wrapper
        imgAddEvent = view.imgAddEvent
        mMainRL = view.mainRL
        mMainRL.setOnTouchListener(this)
        mDayCode = requireArguments().getString(DAY_CODE)!!
        mConfig = requireContext().config
        storeStateVariables()

//        setupButtons()
        mCalendar = MonthlyCalendarImpl(this, requireContext())

        imgAddEvent.setOnClickListener {
            requireContext().launchNewEventIntent(getNewEventDayCode())
        }
        return view
    }

    fun getNewEventDayCode() = Formatter.getTodayCode()

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        if (mConfig.showWeekNumbers != mShowWeekNumbers) {
            mLastHash = -1L
        }

        mCalendar!!.apply {
            mTargetDate = Formatter.getDateTimeFromCode(mDayCode)
            getDays(false)    // prefill the screen asap, even if without events
        }

        storeStateVariables()
        updateCalendar()
    }

    private fun storeStateVariables() {
        mConfig.apply {
            mSundayFirst = isSundayFirst
            mShowWeekNumbers = showWeekNumbers
        }
    }

    fun updateCalendar() {
//        if(isExpand) {
//            expand(mMonthViewWaraper)
//        }else{
//            collapse(mMonthViewWaraper)
//        }
        mCalendar?.updateMonthlyCalendar(Formatter.getDateTimeFromCode(mDayCode))
    }

    companion object {
        var isExpand: Boolean = true

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MonthFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun updateMonthlyCalendar(
        context: Context,
        month: String,
        days: ArrayList<DayMonthly>,
        checkedEvents: Boolean,
        currTargetDate: DateTime
    ) {
        val newHash = month.hashCode() + days.hashCode().toLong()
        if ((mLastHash != 0L && !checkedEvents) || mLastHash == newHash) {
            return
        }

        mLastHash = newHash

        activity?.runOnUiThread {
            mHolder.month_view_wrapper.updateDays(days, false) {
                mSelectedDayCode = it.code
                updateVisibleEvents()
            }
            updateDays(days)
        }

        refreshItems()
    }


    private fun updateDays(days: ArrayList<DayMonthly>) {
        mHolder.month_view_wrapper.updateDays(days, true) {
            (activity as MainActivity).openDayFromMonthly(Formatter.getDateTimeFromCode(it.code))
        }
    }

    fun printCurrentView() {
        mHolder.apply {
            month_view_wrapper.togglePrintMode()

            requireContext().printBitmap(month_calendar_holder.getViewBitmap())

            month_view_wrapper.togglePrintMode()

        }
    }

    private fun updateVisibleEvents() {
        if (activity == null) {
            return
        }

        val filtered = mListEvents.filter {
            if (mSelectedDayCode.isEmpty()) {
                val shownMonthDateTime = Formatter.getDateTimeFromCode(mDayCode)
                val startDateTime = Formatter.getDateTimeFromTS(it.startTS)
                shownMonthDateTime.year == startDateTime.year && shownMonthDateTime.monthOfYear == startDateTime.monthOfYear
            } else {
                val selectionDate = Formatter.getDateTimeFromCode(mSelectedDayCode).toLocalDate()
                val startDate = Formatter.getDateFromTS(it.startTS)
                val endDate = Formatter.getDateFromTS(it.endTS)
                selectionDate in startDate..endDate
            }
        }

        val listItems = requireActivity().getEventListItems(filtered, false)


        activity?.runOnUiThread {
            if (activity != null) {
                mHolder.month_day_events_list.beVisibleIf(listItems.isNotEmpty())
                mHolder.month_day_no_events_placeholder.beVisibleIf(listItems.isEmpty())
                mHolder.topRL.beGoneIf(listItems.isEmpty())

                val currAdapter = mHolder.month_day_events_list.adapter
                if (currAdapter == null) {
                    EventListAdapter(
                        activity as SimpleActivity,
                        listItems,
                        true,
                        this,
                        month_day_events_list
                    ) {
                        if (it is ListEvent) {
                            activity?.editEvent(it)
                        }
                    }.apply {
                        month_day_events_list.adapter = this
                    }
                    month_day_events_list.scheduleLayoutAnimation()
                } else {
                    (currAdapter as EventListAdapter).updateListItems(listItems)
                }
            }
        }
    }

    override fun refreshItems() {
        val startDateTime = Formatter.getLocalDateTimeFromCode(mDayCode).minusWeeks(1)
        val endDateTime = startDateTime.plusWeeks(7)
        activity?.eventsHelper?.getEvents(
            startDateTime.seconds(),
            endDateTime.seconds()
        ) { events ->
            mListEvents = events
            activity?.runOnUiThread {
                updateVisibleEvents()
            }
        }
    }

    fun expand(v: View) {
        isExpand = true
        val matchParentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        v.layoutParams.height = 1

        val a = TranslateAnimation(
            0F,
            0F,
            0F,
            0F
        )
        v.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
        v.requestLayout()
        a.duration = 20000
        a.fillAfter = true
        v.startAnimation(a)
    }

    fun collapse(v: View) {
        isExpand = false

        val a = TranslateAnimation(
            0F,
            0F,
            LinearLayout.LayoutParams.MATCH_PARENT.toFloat(),  // fromYDelta
            0F
        )
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val height: Int = displayMetrics.heightPixels

        v.layoutParams.height = ((height / 2.5).toInt())
        v.requestLayout()
        a.duration = 20000
        a.fillAfter = true
        v.startAnimation(a)

    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_UP -> {
                if (isExpand) {
                    collapse(mMonthViewWaraper)
                    collapse(mMainRL)
                } else {
                    expand(mMonthViewWaraper)
                    expand(mMainRL)
                }
            }
        }
        return true
    }

}
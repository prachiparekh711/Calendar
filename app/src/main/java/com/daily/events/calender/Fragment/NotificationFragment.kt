package com.daily.events.calender.Fragment

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daily.events.calender.Activity.MainActivity
import com.daily.events.calender.Activity.SimpleActivity
import com.daily.events.calender.Adapter.EventListAdapter
import com.daily.events.calender.Extensions.*
import com.daily.events.calender.Model.Event
import com.daily.events.calender.Model.ListItem
import com.daily.events.calender.R
import com.daily.events.calender.helpers.Formatter
import com.daily.events.calender.models.ListEvent
import com.daily.events.calender.models.ListSection
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.MONTH_SECONDS
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyLinearLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.fragment_notification.view.*
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class NotificationFragment : MyFragmentHolder(), RefreshRecyclerViewListener {
    private val NOT_UPDATING = 0
    private val UPDATE_TOP = 1
    private val UPDATE_BOTTOM = 2

    private var FETCH_INTERVAL = 3 * MONTH_SECONDS
    private var MIN_EVENTS_TRESHOLD = 30

    private var mEvents = ArrayList<Event>()
    private var minFetchedTS = 0L
    private var maxFetchedTS = 0L
    private var wereInitialEventsAdded = false
    private var hasBeenScrolled = false
    private var bottomItemAtRefresh: ListItem? = null

    private var use24HourFormat = false

    lateinit var mView: View
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
        mView = inflater.inflate(R.layout.fragment_notification, container, false)
        mView.calendar_events_list_holder?.id = (System.currentTimeMillis() % 100000).toInt()
        mView.calendar_empty_list_placeholder_2.apply {
            setTextColor(context.getAdjustedPrimaryColor())
            setOnClickListener {
                context.launchNewEventIntent(getNewEventDayCode())
            }
        }

        use24HourFormat = requireContext().config.use24HourFormat
        MainActivity.mainBinding?.dateTitleTV?.text = resources.getString(R.string.notification)
        return mView
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NotificationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
        val use24Hour = requireContext().config.use24HourFormat
        if (use24Hour != use24HourFormat) {
            use24HourFormat = use24Hour
            (mView.calendar_events_list.adapter as? EventListAdapter)?.toggle24HourFormat(
                use24HourFormat
            )
        }
    }

    override fun onPause() {
        super.onPause()
        use24HourFormat = requireContext().config.use24HourFormat
    }

    private fun checkEvents() {
        if (!wereInitialEventsAdded) {
            minFetchedTS =
                DateTime().minusMinutes(requireContext().config.displayPastEvents).seconds()
            maxFetchedTS = DateTime().plusMonths(36).seconds()
        }

        requireContext().eventsHelper.getEvents(minFetchedTS, maxFetchedTS) {
            if (it.size >= MIN_EVENTS_TRESHOLD) {
                receivedEvents(it, NOT_UPDATING)
            } else {
                if (!wereInitialEventsAdded) {
                    maxFetchedTS += FETCH_INTERVAL
                }
                requireContext().eventsHelper.getEvents(minFetchedTS, maxFetchedTS) {
                    mEvents = it
                    receivedEvents(mEvents, NOT_UPDATING, !wereInitialEventsAdded)
                }
            }
            wereInitialEventsAdded = true
        }
    }

    var listItems: ArrayList<ListItem> = ArrayList()
    private fun receivedEvents(
        events: ArrayList<Event>,
        updateStatus: Int,
        forceRecreation: Boolean = false
    ) {
        if (context == null || activity == null) {
            return
        }

        mEvents = events
        listItems = requireContext().getNotificationListItems(mEvents)

        activity?.runOnUiThread {
            if (activity == null) {
                return@runOnUiThread
            }

            val currAdapter = mView.calendar_events_list.adapter
            if (currAdapter == null || forceRecreation) {
                EventListAdapter(
                    activity as SimpleActivity,
                    listItems,
                    true,
                    this,
                    mView.calendar_events_list
                ) {
                    if (it is ListEvent) {
                        context?.editEvent(it)
                    }
                }.apply {
                    mView.calendar_events_list.adapter = this
                }

                mView.calendar_events_list.scheduleLayoutAnimation()
                mView.calendar_events_list.endlessScrollListener =
                    object : MyRecyclerView.EndlessScrollListener {
                        override fun updateTop() {
//                            fetchPreviousPeriod()
                        }

                        override fun updateBottom() {
                            fetchNextPeriod()
                        }
                    }

                mView.calendar_events_list.addOnScrollListener(object :
                    RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (!hasBeenScrolled) {
                            hasBeenScrolled = true
                            activity?.invalidateOptionsMenu()
                        }
                    }
                })
            } else {
                (currAdapter as EventListAdapter).updateListItems(listItems)
                if (updateStatus == UPDATE_TOP) {
                    val item = listItems.indexOfFirst { it == bottomItemAtRefresh }
                    if (item != -1) {
                        mView.calendar_events_list.scrollToPosition(item)
                    }
                } else if (updateStatus == UPDATE_BOTTOM) {
                    mView.calendar_events_list.smoothScrollBy(
                        0,
                        requireContext().resources.getDimension(R.dimen.endless_scroll_move_height)
                            .toInt()
                    )
                }
            }
            checkPlaceholderVisibility()
        }
    }

    private fun checkPlaceholderVisibility() {
        mView.calendar_empty_list_placeholder.beVisibleIf(listItems.isEmpty())
        mView.calendar_empty_list_placeholder_2.beVisibleIf(listItems.isEmpty())
        mView.calendar_events_list.beGoneIf(listItems.isEmpty())

    }

    private fun fetchPreviousPeriod() {
        val lastPosition =
            (mView.calendar_events_list.layoutManager as MyLinearLayoutManager).findLastVisibleItemPosition()
        bottomItemAtRefresh =
            (mView.calendar_events_list.adapter as EventListAdapter).listItems[lastPosition]

        val oldMinFetchedTS = minFetchedTS - 1
        minFetchedTS -= FETCH_INTERVAL
        requireContext().eventsHelper.getEvents(minFetchedTS, oldMinFetchedTS) {
            mEvents.addAll(0, it)
            receivedEvents(mEvents, UPDATE_TOP)
        }
    }

    private fun fetchNextPeriod() {
        val oldMaxFetchedTS = maxFetchedTS + 1
        maxFetchedTS += FETCH_INTERVAL
        requireContext().eventsHelper.getEvents(oldMaxFetchedTS, maxFetchedTS) {
            mEvents.addAll(it)
            receivedEvents(mEvents, UPDATE_BOTTOM)
        }
    }

    override fun refreshItems() {
        checkEvents()
    }

    override fun goToToday() {
        val listItems = requireContext().getEventListItems(mEvents)
        val firstNonPastSectionIndex =
            listItems.indexOfFirst { it is ListSection && !it.isPastSection }
        if (firstNonPastSectionIndex != -1) {
            (mView.calendar_events_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                firstNonPastSectionIndex,
                0
            )
            mView.calendar_events_list.onGlobalLayout {
                hasBeenScrolled = false
                activity?.invalidateOptionsMenu()
            }
        }
    }

    override fun showGoToDateDialog() {}

    override fun refreshEvents() {
        checkEvents()
    }

    override fun shouldGoToTodayBeVisible() = hasBeenScrolled

    override fun updateActionBarTitle() {
        (activity as? MainActivity)?.updateActionBarTitle(getString(R.string.app_launcher_name))
    }

    override fun getNewEventDayCode() = Formatter.getTodayCode()

    override fun printView() {
        mView.apply {
            if (calendar_events_list.isGone()) {
                context.toast(R.string.no_items_found)
                return@apply
            }

            (calendar_events_list.adapter as? EventListAdapter)?.togglePrintMode()
            Handler().postDelayed({
                requireContext().printBitmap(calendar_events_list.getViewBitmap())

                Handler().postDelayed({
                    (calendar_events_list.adapter as? EventListAdapter)?.togglePrintMode()
                }, 1000)
            }, 1000)
        }
    }
}
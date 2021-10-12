package com.daily.events.calender.Activity

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.*
import android.database.ContentObserver
import android.os.*
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.loader.content.CursorLoader
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.daily.events.calender.Extensions.*
import com.daily.events.calender.Fragment.*
import com.daily.events.calender.Fragment.Home.HomeFragment
import com.daily.events.calender.Model.Event
import com.daily.events.calender.Model.EventType
import com.daily.events.calender.R
import com.daily.events.calender.SharedPrefrences
import com.daily.events.calender.databinding.ActivityMainBinding
import com.daily.events.calender.dialogs.SetRemindersDialog
import com.daily.events.calender.helpers.*
import com.daily.events.calender.helpers.Formatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.shrikanthravi.customnavigationdrawer2.widget.SNavigationDrawer
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.SimpleContact
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.calendar_item_account.view.*
import kotlinx.android.synthetic.main.calendar_item_calendar.view.*
import kotlinx.android.synthetic.main.dialog_select_calendars.view.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import pub.devrel.easypermissions.AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : SimpleActivity() {
    //    var fragmentClass: Intrinsics.Kotlin<*>? = null
    private var selectAccountReceiver: SelectAccountReceiver? = null
    var IsSet = false
    var homeFragment: HomeFragment? = null
    var eventFragment: EventFragment? = null
    var notificationFragment: NotificationFragment? = null
    var settingFragment: SettingFragment? = null

    var menuItem: Int = 0


    companion object {
        var isPaused: Boolean = false
        var fragment: Fragment? = null
        val CALDAV_REFRESH_DELAY = 3000L
        val calDAVRefreshHandler = Handler()
        var calDAVRefreshCallback: (() -> Unit)? = null

        lateinit var selectAccountBehaviour: BottomSheetBehavior<LinearLayout>
        lateinit var syncCalendarBehaviour: BottomSheetBehavior<LinearLayout>


        var mainBinding: ActivityMainBinding? = null

        lateinit var activity: Activity

        fun getSyncedCalDAVCalendars() =
            activity.calDAVHelper.getCalDAVCalendars(activity.config.caldavSyncedCalendarIds, false)


        fun syncCalDAVCalendars(callback: () -> Unit) {
            calDAVRefreshCallback = callback
            ensureBackgroundThread {
                val uri = CalendarContract.Calendars.CONTENT_URI
                activity.contentResolver.unregisterContentObserver(calDAVSyncObserver)
                activity.contentResolver.registerContentObserver(uri, false, calDAVSyncObserver)
                activity.refreshCalDAVCalendars(activity.config.caldavSyncedCalendarIds, true)
            }
        }

        private val calDAVSyncObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                if (!selfChange) {
                    calDAVRefreshHandler.removeCallbacksAndMessages(null)
                    calDAVRefreshHandler.postDelayed({
                        ensureBackgroundThread {
                            unregisterObserver()
                            calDAVRefreshCallback?.invoke()
                            calDAVRefreshCallback = null
                        }
                    }, CALDAV_REFRESH_DELAY)
                }
            }
        }

        fun unregisterObserver() {
            activity.contentResolver.unregisterContentObserver(calDAVSyncObserver)
        }

        private fun updateDefaultEventTypeText() {
            if (activity.config.defaultEventTypeId == -1L) {

            } else {
                ensureBackgroundThread {
                    val eventType =
                        activity.eventTypesDB.getEventTypeWithId(activity.config.defaultEventTypeId)
                    if (eventType != null) {
                        activity.config.lastUsedCaldavCalendarId = eventType.caldavCalendarId
                    } else {
                        activity.config.defaultEventTypeId = -1
                        updateDefaultEventTypeText()
                    }
                }
            }
        }

        fun getMyContactsCursor(favoritesOnly: Boolean, withPhoneNumbersOnly: Boolean) = try {
            val getFavoritesOnly = if (favoritesOnly) "1" else "0"
            val getWithPhoneNumbersOnly = if (withPhoneNumbersOnly) "1" else "0"
            val args = arrayOf(getFavoritesOnly, getWithPhoneNumbersOnly)
            CursorLoader(
                activity,
                MyContactsContentProvider.CONTACTS_CONTENT_URI,
                null,
                null,
                args,
                null
            )
        } catch (e: Exception) {
            null
        }

        fun updateWidgets() {
            val widgetIDs = AppWidgetManager.getInstance(activity)?.getAppWidgetIds(
                ComponentName(
                    activity, MyWidgetMonthlyProvider::class.java
                )
            )
                ?: return
            if (widgetIDs.isNotEmpty()) {
                Intent(activity, MyWidgetMonthlyProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
                    activity.sendBroadcast(this)
                }
            }

            updateListWidget()
            updateDateWidget()
        }

        fun updateListWidget() {
            val widgetIDs = AppWidgetManager.getInstance(activity)
                ?.getAppWidgetIds(ComponentName(activity, MyWidgetListProvider::class.java))
                ?: return
            if (widgetIDs.isNotEmpty()) {
                Intent(activity, MyWidgetListProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
                    activity.sendBroadcast(this)
                }
            }
        }

        fun updateDateWidget() {
            val widgetIDs = AppWidgetManager.getInstance(activity)
                ?.getAppWidgetIds(ComponentName(activity, MyWidgetDateProvider::class.java))
                ?: return
            if (widgetIDs.isNotEmpty()) {
                Intent(activity, MyWidgetDateProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
                    activity.sendBroadcast(this)
                }
            }
        }

        private fun addPrivateEvents(
            birthdays: Boolean,
            contacts: ArrayList<SimpleContact>,
            reminders: ArrayList<Int>,
            callback: (eventsFound: Int, eventsAdded: Int) -> Unit
        ) {
            var eventsAdded = 0
            var eventsFound = 0
            if (contacts.isEmpty()) {
                callback(0, 0)
                return
            }

            try {
                val eventTypeId =
                    if (birthdays) activity.eventsHelper.getBirthdaysEventTypeId() else activity.eventsHelper.getAnniversariesEventTypeId()
                val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else SOURCE_CONTACT_ANNIVERSARY

                val existingEvents =
                    if (birthdays) activity.eventsDB.getBirthdays() else activity.eventsDB.getAnniversaries()
                val importIDs = HashMap<String, Long>()
                existingEvents.forEach {
                    importIDs[it.importId] = it.startTS
                }

                contacts.forEach { contact ->
                    val events = if (birthdays) contact.birthdays else contact.anniversaries
                    events.forEach { birthdayAnniversary ->
                        // private contacts are created in Simple Contacts Pro, so we can guarantee that they exist only in these 2 formats
                        val format = if (birthdayAnniversary.startsWith("--")) {
                            "--MM-dd"
                        } else {
                            "yyyy-MM-dd"
                        }

                        val formatter = SimpleDateFormat(format, Locale.getDefault())
                        val date = formatter.parse(birthdayAnniversary)
                        if (date.year < 70) {
                            date.year = 70
                        }

                        val timestamp = date.time / 1000L
                        val lastUpdated = System.currentTimeMillis()
                        val event = Event(
                            null,
                            timestamp,
                            timestamp,
                            contact.name,
                            reminder1Minutes = reminders[0],
                            reminder2Minutes = reminders[1],
                            reminder3Minutes = reminders[2],
                            importId = contact.contactId.toString(),
                            timeZone = DateTimeZone.getDefault().id,
                            flags = FLAG_ALL_DAY,
                            repeatInterval = YEAR,
                            repeatRule = REPEAT_SAME_DAY,
                            eventType = eventTypeId,
                            source = source,
                            lastUpdated = lastUpdated
                        )

                        val importIDsToDelete = ArrayList<String>()
                        for ((key, value) in importIDs) {
                            if (key == contact.contactId.toString() && value != timestamp) {
                                val deleted =
                                    activity.eventsDB.deleteBirthdayAnniversary(source, key)
                                if (deleted == 1) {
                                    importIDsToDelete.add(key)
                                }
                            }
                        }

                        importIDsToDelete.forEach {
                            importIDs.remove(it)
                        }

                        eventsFound++
                        if (!importIDs.containsKey(contact.contactId.toString())) {
                            activity.eventsHelper.insertEvent(event, false, false) {
                                eventsAdded++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LLL_Error: ", "Add Contact ${e.localizedMessage}")
            }

            callback(eventsFound, eventsAdded)
        }

        private fun addContactEvents(
            birthdays: Boolean,
            reminders: ArrayList<Int>,
            initEventsFound: Int,
            initEventsAdded: Int,
            callback: (Int) -> Unit
        ) {
            var eventsFound = initEventsFound
            var eventsAdded = initEventsAdded
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Event.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP,
                ContactsContract.CommonDataKinds.Event.START_DATE
            )

            val selection =
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
            val type =
                if (birthdays) ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY else ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY
            val selectionArgs =
                arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, type.toString())

            val dateFormats = getDateFormats()
            val yearDateFormats = getDateFormatsWithYear()
            val existingEvents =
                if (birthdays) activity.eventsDB.getBirthdays() else activity.eventsDB.getAnniversaries()
            val importIDs = HashMap<String, Long>()
            existingEvents.forEach {
                importIDs[it.importId] = it.startTS
            }

            val eventTypeId =
                if (birthdays) activity.eventsHelper.getBirthdaysEventTypeId() else activity.eventsHelper.getAnniversariesEventTypeId()
            val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else SOURCE_CONTACT_ANNIVERSARY

            activity.queryCursor(
                uri,
                projection,
                selection,
                selectionArgs,
                showErrors = true
            ) { cursor ->
                val contactId =
                    cursor.getIntValue(ContactsContract.CommonDataKinds.Event.CONTACT_ID).toString()
                val name = cursor.getStringValue(ContactsContract.Contacts.DISPLAY_NAME)
                val startDate =
                    cursor.getStringValue(ContactsContract.CommonDataKinds.Event.START_DATE)

                for (format in dateFormats) {
                    try {
                        val formatter = SimpleDateFormat(format, Locale.getDefault())
                        val date = formatter.parse(startDate)
                        val flags = if (format in yearDateFormats) {
                            FLAG_ALL_DAY
                        } else {
                            FLAG_ALL_DAY or FLAG_MISSING_YEAR
                        }

                        val timestamp = date.time / 1000L
                        val lastUpdated =
                            cursor.getLongValue(ContactsContract.CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP)
                        val event = Event(
                            null,
                            timestamp,
                            timestamp,
                            name,
                            reminder1Minutes = reminders[0],
                            reminder2Minutes = reminders[1],
                            reminder3Minutes = reminders[2],
                            importId = contactId,
                            timeZone = DateTimeZone.getDefault().id,
                            flags = flags,
                            repeatInterval = YEAR,
                            repeatRule = REPEAT_SAME_DAY,
                            eventType = eventTypeId,
                            source = source,
                            lastUpdated = lastUpdated
                        )

                        val importIDsToDelete = ArrayList<String>()
                        for ((key, value) in importIDs) {
                            if (key == contactId && value != timestamp) {
                                val deleted =
                                    activity.eventsDB.deleteBirthdayAnniversary(source, key)
                                if (deleted == 1) {
                                    importIDsToDelete.add(key)
                                }
                            }
                        }

                        importIDsToDelete.forEach {
                            importIDs.remove(it)
                        }

                        eventsFound++
                        if (!importIDs.containsKey(contactId)) {
                            activity.eventsHelper.insertEvent(event, false, false) {
                                eventsAdded++
                            }
                        }
                        break
                    } catch (e: Exception) {
                    }
                }
            }

            activity.runOnUiThread {
                callback(if (eventsAdded == 0 && eventsFound > 0) -1 else eventsAdded)
            }
        }

        private fun setupQuickFilter(isAdded: Boolean) {
            activity.eventsHelper.getEventTypes(activity, false) {
                activity.config.displayEventTypes.plus(
                    activity.eventsHelper.getBirthdaysEventTypeId().toString()
                )
                updateWidgets()
            }
        }

        private fun setupQuicAnniversaryFilter() {
            activity.eventsHelper.getEventTypes(activity, false) {
                activity.config.displayEventTypes.plus(
                    activity.eventsHelper.getAnniversariesEventTypeId().toString()
                )
                updateWidgets()
            }
        }

    }


    private var showCalDAVRefreshToast = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        supportActionBar?.hide()

        activity = this

        if (config.caldavSync) {
            refreshCalDAVCalendars(false)
        }

        selectAccountBehaviour =
            BottomSheetBehavior.from(llBottom)

        syncCalendarBehaviour =
            BottomSheetBehavior.from(llBottomSync)

        syncCalendarBehaviour.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    mainBinding?.hideBack?.beVisible()
                } else {
                    mainBinding?.hideBack?.beGone()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        })

        selectAccountBehaviour.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    mainBinding?.hideBack?.beGone()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        })

        selectAccountReceiver = SelectAccountReceiver()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            selectAccountReceiver!!,
            IntentFilter("OPEN_ACCOUNT_SYNC")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            holidayReceiver,
            IntentFilter("ADD_HOLIDAYS")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            birthdayReceiver,
            IntentFilter("ADD_BIRTHDAY")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            anniversaryReceiver,
            IntentFilter("ADD_ANNIVERSARY")
        )

        config.isSundayFirst = false

        mainBinding?.fab?.setOnClickListener {
            launchNewEventIntent(getNewEventDayCode())
        }
    }

    fun getNewEventDayCode() = Formatter.getTodayCode()

    override fun permissionGranted() {
        if (!IsSet) SetFragments()
    }

    fun SetFragments() {

        if (!SharedPrefrences.getIntro(this)) {
            if (mainBinding?.lottieLayerName?.visibility == View.GONE) {
                mainBinding?.lottieLayerName?.visibility = View.VISIBLE
                mainBinding?.lottieText?.visibility = View.VISIBLE
                mainBinding?.hideBack?.visibility = View.VISIBLE
            }
        }

        mainBinding?.hideBack?.setOnTouchListener { _, _ ->
            if (mainBinding?.lottieLayerName?.visibility == View.VISIBLE) {
                activity.runOnUiThread {
                    mainBinding?.lottieLayerName?.beGone()
                    mainBinding?.lottieText?.beGone()
                    mainBinding?.hideBack?.beGone()
                }

                if (!SharedPrefrences.getIntro(activity)) {

                    SharedPrefrences.setIntro(this@MainActivity, true)
                    if (!SharedPrefrences.getUser(this@MainActivity)) {
                        syncCalendarBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                        SharedPrefrences.setUser(this@MainActivity, true)
                        mainBinding?.dialogNotNow?.setOnClickListener {
                            mainBinding?.hideBack?.beGone()
                            syncCalendarBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                        mainBinding?.dialogSync?.setOnClickListener {
                            syncCalendarBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                            val lbm = LocalBroadcastManager.getInstance(this@MainActivity)
                            val localIn = Intent("OPEN_ACCOUNT_SYNC")
                            lbm.sendBroadcast(localIn)
                            selectAccountBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }
                }
            }
            true
        }

        IsSet = true
        homeFragment = HomeFragment()
        eventFragment = EventFragment()
        notificationFragment = NotificationFragment()
        settingFragment = SettingFragment()
        setNavigationItems()
        homeFragment?.let {
            supportFragmentManager.beginTransaction().replace(R.id.container, it)
                .commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEFAULT_SETTINGS_REQ_CODE) {

            // Do something after user returned from app settings screen, like showing a Toast.
            if (EasyPermissions.hasPermissions(this, *perms)) {
                permissionGranted()
            } else {
                EasyPermissions.requestPermissions(
                    this, getString(R.string.permission_str),
                    RC_READ_EXTERNAL_STORAGE, *perms
                )
            }
        }
        if (requestCode == 2296) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                Log.e("hasPermissions", "true")
                if (Environment.isExternalStorageManager()) {
                    permissionGranted()
                } else {
                    EasyPermissions.requestPermissions(
                        this, getString(R.string.permission_str),
                        RC_READ_EXTERNAL_STORAGE, *perms
                    )
                    //                    Toasty.info(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private fun refreshCalDAVCalendars(showRefreshToast: Boolean) {
        showCalDAVRefreshToast = showRefreshToast
        if (showRefreshToast) {
            toast(R.string.refreshing)
        }

        syncCalDAVCalendars {
            calDAVHelper.refreshCalendars(true) {
                calDAVChanged()
            }
        }
    }

    private fun calDAVChanged() {
        if (showCalDAVRefreshToast) {
            toast(R.string.refreshing_complete)
        }
    }

    override fun onBackPressed() {

        if (selectAccountBehaviour.state == BottomSheetBehavior.STATE_EXPANDED) {
            selectAccountBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            mainBinding?.hideBack?.beGone()
        }

        if (syncCalendarBehaviour.state == BottomSheetBehavior.STATE_EXPANDED) {
            syncCalendarBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            mainBinding?.hideBack?.beGone()
        }

        if (menuItem != 0) {
            homeFragment?.let {
                supportFragmentManager.beginTransaction().replace(R.id.container, it)
                    .commit()
            }
            mainBinding?.fab?.visibility = View.VISIBLE
            mainBinding?.today?.visibility = View.VISIBLE
            mainBinding?.today?.beVisible()
            menuItem = 0
        } else {
            super.onBackPressed()
        }

    }

    fun setNavigationItems() {
        val menuItems: MutableList<com.shrikanthravi.customnavigationdrawer2.data.MenuItem> =
            ArrayList()
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Home",
                R.drawable.ic_side_select,
                R.drawable.ic_home_new
            )
        )
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Event",
                R.drawable.ic_side_select,
                R.drawable.ic_event_new
            )
        )
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Notification",
                R.drawable.ic_side_select,
                R.drawable.ic_notification_new
            )
        )
        menuItems.add(
            com.shrikanthravi.customnavigationdrawer2.data.MenuItem(
                "Setting",
                R.drawable.ic_side_select,
                R.drawable.ic_setting_new
            )
        )
        mainBinding?.navigationDrawer?.menuItemList = menuItems

        try {
            fragment = HomeFragment()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        val fragmentManager: FragmentManager = supportFragmentManager
        fragment?.let {
            fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .replace(
                    R.id.container,
                    it
                ).commit()
        }

        mainBinding?.navigationDrawer?.onMenuItemClickListener =
            SNavigationDrawer.OnMenuItemClickListener { position ->
                menuItem = position
                when (position) {
                    0 -> {
                        mainBinding?.fab?.visibility = View.VISIBLE
                        mainBinding?.today?.visibility = View.VISIBLE
                        fragment = HomeFragment()
                    }
                    1 -> {
                        mainBinding?.fab?.visibility = View.GONE
                        mainBinding?.today?.visibility = View.GONE
                        fragment = EventFragment()
                    }
                    2 -> {
                        mainBinding?.fab?.visibility = View.GONE
                        mainBinding?.today?.visibility = View.GONE
                        fragment = NotificationFragment()
                    }
                    3 -> {
                        mainBinding?.fab?.visibility = View.GONE
                        mainBinding?.today?.visibility = View.GONE
                        fragment = SettingFragment()
                    }
                }
                mainBinding?.navigationDrawer?.drawerListener =
                    object : SNavigationDrawer.DrawerListener {
                        override fun onDrawerOpened() {

                        }

                        override fun onDrawerOpening() {
                            if (position != 0) {
                                mainBinding?.topRL?.visibility = View.GONE
                            } else {
                                mainBinding?.topRL?.visibility = View.VISIBLE
                            }
                        }

                        override fun onDrawerClosing() {
                            println("Drawer closed")
                            mainBinding?.topRL?.visibility = View.VISIBLE
                            if (fragment != null) {
                                val fragmentManager = supportFragmentManager
                                fragmentManager.beginTransaction().setCustomAnimations(
                                    android.R.animator.fade_in,
                                    android.R.animator.fade_out
                                ).replace(
                                    R.id.container,
                                    fragment!!
                                ).commit()
                            }
                        }

                        override fun onDrawerClosed() {}
                        override fun onDrawerStateChanged(newState: Int) {
                            println("State $newState")
                        }
                    }
            }
    }

    fun openMonthFromYearly(dateTime: DateTime) {
        val fragment = MonthFragmentsHolder()
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(R.id.container1, fragment).commitNow()
        homeFragment?.monthChanges()
    }

    fun openDayFromMonthly(dateTime: DateTime) {
//        val fragment = DayFragmentsHolder()
//        val bundle = Bundle()
//        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
//        fragment.arguments = bundle
//        try {
//            supportFragmentManager.beginTransaction().replace(R.id.container1, fragment).commitNow()
//            homeFragment?.dayChanges()
//        } catch (e: Exception) {
//        }
    }

    fun toggleGoToTodayVisibility(beVisible: Boolean) {
//        shouldGoToTodayBeVisible = beVisible
//        if (goToTodayButton?.isVisible != beVisible) {
//            invalidateOptionsMenu()
//        }
    }

    private class SelectAccountReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            var prevAccount = ""

            selectAccountBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
            mainBinding?.hideBack?.beVisible()

            val ids = context.config.getSyncedCalendarIdsAsList()
            val calendars = context.calDAVHelper.getCalDAVCalendars("", true)
            mainBinding?.llMain?.dialogSelectCalendarsPlaceholder?.beVisibleIf(calendars.isEmpty())
            mainBinding?.llMain?.dialogSelectCalendarsHolder?.beVisibleIf(calendars.isNotEmpty())

            mainBinding?.llMain?.dialogSelectCalendarsHolder?.removeAllViews()

            val sorted = calendars.sortedWith(compareBy({ it.accountName }, { it.displayName }))
            sorted.forEach {
                if (prevAccount != it.accountName) {
                    prevAccount = it.accountName
                    addCalendarItem(false, it.accountName)
                }

                addCalendarItem(true, it.displayName, it.id, ids.contains(it.id))
            }

            mainBinding?.llMain?.dialogSubmit?.setOnClickListener {
                Log.e(
                    "LLL_Bool: ",
                    mainBinding?.llMain?.calendarItemBirthdaySwitch?.isChecked.toString()
                )
                confirmSelection(mainBinding?.llMain?.calendarItemBirthdaySwitch?.isChecked!!)
            }

            mainBinding?.llMain?.dialogCancel?.setOnClickListener {
                selectAccountBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                mainBinding?.hideBack?.beGone()
            }
        }

        fun addCalendarItem(
            isEvent: Boolean,
            text: String,
            tag: Int = 0,
            shouldCheck: Boolean = false
        ) {
            val layout =
                if (isEvent) R.layout.calendar_item_calendar else R.layout.calendar_item_account
            val calendarItem = activity.layoutInflater.inflate(
                layout,
                mainBinding?.llMain?.dialogSelectCalendarsHolder,
                false
            )

            mainBinding?.llMain?.calendarItemBirthdaySwitch?.apply {
                this.text = "Add Contact Birthday"
                isChecked = shouldCheck
                mainBinding?.llMain?.calendarItemBirthdaySwitch?.setOnClickListener {
                    toggle()
                }
            }

            mainBinding?.llMain?.calendarItemAnniversarySwitch?.apply {
                this.text = "Add Contact Anniversary"
                isChecked = shouldCheck
                mainBinding?.llMain?.calendarItemBirthdaySwitch?.setOnClickListener {
                    toggle()
                }
            }

            if (isEvent) {
                calendarItem.calendar_item_calendar_switch.apply {
                    this.tag = tag
                    this.text = text
                    isChecked = shouldCheck
                    calendarItem.setOnClickListener {
                        toggle()
                    }
                }
            } else {
                calendarItem.calendar_item_account.text = text
            }

            mainBinding?.llMain?.dialogSelectCalendarsHolder?.addView(calendarItem)

        }

        private fun confirmSelection(isAdded: Boolean) {
            val oldCalendarIds = activity.config.getSyncedCalendarIdsAsList()
            val calendarIds = ArrayList<Int>()
            val childCnt = mainBinding?.llMain?.dialogSelectCalendarsHolder?.childCount
            for (i in 0..childCnt!!) {
                val child = mainBinding?.llMain?.dialogSelectCalendarsHolder?.getChildAt(i)
                if (child is RelativeLayout) {
                    val check = child.getChildAt(0)
                    if (check is SwitchCompat && check.isChecked) {
                        calendarIds.add(check.tag as Int)
                    }
                }
            }
            activity.config.caldavSyncedCalendarIds = TextUtils.join(",", calendarIds)

            val newCalendarIds = activity.config.getSyncedCalendarIdsAsList()

            activity.config.caldavSync = newCalendarIds.isNotEmpty()
            if (newCalendarIds.isNotEmpty()) {
                Toast.makeText(
                    activity,
                    activity.resources.getString(R.string.syncing),
                    Toast.LENGTH_SHORT
                ).show()
            }

            ensureBackgroundThread {
                if (newCalendarIds.isNotEmpty()) {
                    val existingEventTypeNames = activity.eventsHelper.getEventTypesSync().map {
                        it.getDisplayTitle()
                            .lowercase(Locale.getDefault())
                    } as ArrayList<String>
                    getSyncedCalDAVCalendars().forEach {
                        val calendarTitle = it.getFullTitle()
                        if (!existingEventTypeNames.contains(calendarTitle.lowercase(Locale.getDefault()))) {
                            val eventType = EventType(
                                null,
                                it.displayName,
                                it.color,
                                it.id,
                                it.displayName,
                                it.accountName
                            )
                            existingEventTypeNames.add(calendarTitle.lowercase(Locale.getDefault()))
                            activity.eventsHelper.insertOrUpdateEventType(activity, eventType)
                        }
                    }

                    syncCalDAVCalendars {
                        activity.calDAVHelper.refreshCalendars(true) {
                            if (newCalendarIds.isNotEmpty()) {
                                activity.runOnUiThread {
                                    Toast.makeText(
                                        activity,
                                        activity.resources.getString(R.string.synchronization_completed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }

                val removedCalendarIds = oldCalendarIds.filter { !newCalendarIds.contains(it) }
                removedCalendarIds.forEach {
                    activity.calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                    activity.eventsHelper.getEventTypeWithCalDAVCalendarId(it)?.apply {
                        activity.eventsHelper.deleteEventTypes(arrayListOf(this), true)
                    }
                }

                activity.eventTypesDB.deleteEventTypesWithCalendarId(removedCalendarIds)
                updateDefaultEventTypeText()
            }
            selectAccountBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            mainBinding?.hideBack?.beGone()
        }
    }

    class AddBirthdayTask : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String {
            tryAddBirthdays(true)
            return ""
        }

        fun tryAddBirthdays(isAdded: Boolean) {
            val isGranted = EasyPermissions.hasPermissions(activity, *perms)
            if (isGranted) {
                activity.runOnUiThread {
                    SetRemindersDialog(activity) {
                        val reminders = it
                        val privateCursor =
                            Companion.getMyContactsCursor(false, false)?.loadInBackground()

                        ensureBackgroundThread {
                            val privateContacts =
                                MyContactsContentProvider.getSimpleContacts(activity, privateCursor)
                            addPrivateEvents(
                                true,
                                privateContacts,
                                reminders
                            ) { eventsFound, eventsAdded ->
                                addContactEvents(true, reminders, eventsFound, eventsAdded) {
                                    when {
                                        it > 0 -> {
                                            Toast.makeText(
                                                activity,
                                                R.string.birthdays_added,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            setupQuickFilter(isAdded)
                                        }
                                        it == -1 -> Toast.makeText(
                                            activity,
                                            R.string.no_new_birthdays,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        else -> Toast.makeText(
                                            activity,
                                            R.string.no_birthdays,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(
                    activity,
                    R.string.no_contacts_permission,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    class AddAnniversaryTask : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String {
            tryAddAnniversaries()
            return ""
        }

        private fun tryAddAnniversaries() {
            val isGranted = EasyPermissions.hasPermissions(activity, *perms)
            if (isGranted) {
                activity.runOnUiThread {
                    SetRemindersDialog(activity) {
                        val reminders = it
                        val privateCursor = getMyContactsCursor(false, false)?.loadInBackground()

                        ensureBackgroundThread {
                            val privateContacts =
                                MyContactsContentProvider.getSimpleContacts(activity, privateCursor)
                            addPrivateEvents(
                                false,
                                privateContacts,
                                reminders
                            ) { eventsFound, eventsAdded ->
                                addContactEvents(false, reminders, eventsFound, eventsAdded) {
                                    when {
                                        it > 0 -> {
                                            Toast.makeText(
                                                activity,
                                                R.string.anniversaries_added,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            setupQuicAnniversaryFilter()
                                        }
                                        it == -1 -> Toast.makeText(
                                            activity,
                                            R.string.no_new_anniversaries,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        else -> Toast.makeText(
                                            activity,
                                            R.string.no_anniversaries,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(
                    activity,
                    R.string.no_contacts_permission,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    /******************** Add holidays *************************/
    private val holidayReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            addHolidays()
        }
    }

    private val birthdayReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AddBirthdayTask().execute()
        }
    }

    private val anniversaryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AddAnniversaryTask().execute()
        }
    }

    private fun addHolidays() {
        val items = getHolidayRadioItems()
        RadioGroupDialog(this, items) {
            toast(R.string.importing)
            ensureBackgroundThread {
                val holidays = getString(R.string.holidays)
                var eventTypeId = eventsHelper.getEventTypeIdWithTitle(holidays)
                if (eventTypeId == -1L) {
                    val eventType = EventType(
                        null,
                        holidays,
                        resources.getColor(R.color.default_holidays_color)
                    )
                    eventTypeId = eventsHelper.insertOrUpdateEventTypeSync(eventType)
                }

                val result = IcsImporter(this).importEvents(it as String, eventTypeId, 0, false)
                handleParseResult(result)
            }
        }
    }

    private fun getHolidayRadioItems(): ArrayList<RadioItem> {
        val items = ArrayList<RadioItem>()

        LinkedHashMap<String, String>().apply {
            put("Algeria", "algeria.ics")
            put("Argentina", "argentina.ics")
            put("Australia", "australia.ics")
            put("België", "belgium.ics")
            put("Bolivia", "bolivia.ics")
            put("Brasil", "brazil.ics")
            put("България", "bulgaria.ics")
            put("Canada", "canada.ics")
            put("China", "china.ics")
            put("Colombia", "colombia.ics")
            put("Česká republika", "czech.ics")
            put("Danmark", "denmark.ics")
            put("Deutschland", "germany.ics")
            put("Eesti", "estonia.ics")
            put("España", "spain.ics")
            put("Éire", "ireland.ics")
            put("France", "france.ics")
            put("Fürstentum Liechtenstein", "liechtenstein.ics")
            put("Hellas", "greece.ics")
            put("Hrvatska", "croatia.ics")
            put("India", "india.ics")
            put("Indonesia", "indonesia.ics")
            put("Ísland", "iceland.ics")
            put("Israel", "israel.ics")
            put("Italia", "italy.ics")
            put("Қазақстан Республикасы", "kazakhstan.ics")
            put("المملكة المغربية", "morocco.ics")
            put("Latvija", "latvia.ics")
            put("Lietuva", "lithuania.ics")
            put("Luxemburg", "luxembourg.ics")
            put("Makedonija", "macedonia.ics")
            put("Malaysia", "malaysia.ics")
            put("Magyarország", "hungary.ics")
            put("México", "mexico.ics")
            put("Nederland", "netherlands.ics")
            put("República de Nicaragua", "nicaragua.ics")
            put("日本", "japan.ics")
            put("Nigeria", "nigeria.ics")
            put("Norge", "norway.ics")
            put("Österreich", "austria.ics")
            put("Pākistān", "pakistan.ics")
            put("Polska", "poland.ics")
            put("Portugal", "portugal.ics")
            put("Россия", "russia.ics")
            put("República de Costa Rica", "costarica.ics")
            put("República Oriental del Uruguay", "uruguay.ics")
            put("République d'Haïti", "haiti.ics")
            put("România", "romania.ics")
            put("Schweiz", "switzerland.ics")
            put("Singapore", "singapore.ics")
            put("한국", "southkorea.ics")
            put("Srbija", "serbia.ics")
            put("Slovenija", "slovenia.ics")
            put("Slovensko", "slovakia.ics")
            put("South Africa", "southafrica.ics")
            put("Suomi", "finland.ics")
            put("Sverige", "sweden.ics")
            put("Taiwan", "taiwan.ics")
            put("ราชอาณาจักรไทย", "thailand.ics")
            put("Türkiye Cumhuriyeti", "turkey.ics")
            put("Ukraine", "ukraine.ics")
            put("United Kingdom", "unitedkingdom.ics")
            put("United States", "unitedstates.ics")

            var i = 0
            for ((country, file) in this) {
                items.add(RadioItem(i++, country, file))
            }
        }

        return items
    }

    private fun handleParseResult(result: IcsImporter.ImportResult) {
        toast(
            when (result) {
                IcsImporter.ImportResult.IMPORT_NOTHING_NEW -> R.string.no_new_items
                IcsImporter.ImportResult.IMPORT_OK -> R.string.holidays_imported_successfully
                IcsImporter.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_holidays_failed
                else -> R.string.importing_holidays_failed
            }, Toast.LENGTH_LONG
        )
    }

}
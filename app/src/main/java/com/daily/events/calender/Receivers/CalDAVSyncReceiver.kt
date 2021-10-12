package com.daily.events.calender.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daily.events.calender.Extensions.config
import com.daily.events.calender.Extensions.recheckCalDAVCalendars
import com.daily.events.calender.Extensions.refreshCalDAVCalendars
import com.daily.events.calender.Extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.config.caldavSync) {
            context.refreshCalDAVCalendars(context.config.caldavSyncedCalendarIds, false)
        }

        context.recheckCalDAVCalendars {
            context.updateWidgets()
        }
    }
}

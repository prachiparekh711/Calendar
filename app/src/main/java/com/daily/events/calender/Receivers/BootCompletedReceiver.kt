package com.daily.events.calender.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daily.events.calender.Extensions.notifyRunningEvents
import com.daily.events.calender.Extensions.recheckCalDAVCalendars
import com.daily.events.calender.Extensions.scheduleAllEvents
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            context.apply {
                scheduleAllEvents()
                notifyRunningEvents()
                recheckCalDAVCalendars {}
            }
        }
    }
}

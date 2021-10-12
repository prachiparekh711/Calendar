package com.daily.events.calender.services

import android.app.IntentService
import android.content.Intent
import com.daily.events.calender.Extensions.config
import com.daily.events.calender.Extensions.eventsDB
import com.daily.events.calender.Extensions.rescheduleReminder
import com.daily.events.calender.helpers.EVENT_ID

class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val eventId = intent.getLongExtra(EVENT_ID, 0L)
            val event = eventsDB.getEventWithId(eventId)
            rescheduleReminder(event, config.snoozeTime)
        }
    }
}

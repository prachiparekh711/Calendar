package com.daily.events.calender.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.*

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val calendar = Calendar.getInstance()
            AlarmReceiver().setRepeatAlarm(context, 99526463, calendar)
        }
    }

}
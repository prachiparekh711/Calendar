package com.daily.events.calender.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    var mAlarmManager: AlarmManager? = null
    var mPendingIntent: PendingIntent? = null
    var mCalendar: Calendar? = null
    override fun onReceive(context: Context, intent: Intent) {
        AlarmService.enqueueWork(context, intent)
    }

    fun setRepeatAlarm(context: Context, ID: Int, calendar: Calendar?) {
        mCalendar = calendar
        mAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Put Reminder ID in Intent Extra
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("id", ID)
        mPendingIntent =
            PendingIntent.getBroadcast(context, ID, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        // Calculate notification timein
        val c = Calendar.getInstance()
        val currentTime = c.timeInMillis
        val diffTime = mCalendar!!.timeInMillis - currentTime
        mAlarmManager!!.setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime(),
            60000, mPendingIntent
        )
//        mAlarmManager!!.setRepeating(
//            AlarmManager.ELAPSED_REALTIME,
//            SystemClock.elapsedRealtime() + diffTime,
//            60000, mPendingIntent
//        )

        // Restart alarm if device is rebooted
        val receiver = ComponentName(
            context,
            BootReceiver::class.java
        )
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

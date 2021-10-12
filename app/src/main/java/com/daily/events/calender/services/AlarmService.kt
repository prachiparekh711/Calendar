package com.daily.events.calender.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.JobIntentService
import com.daily.events.calender.BuildConfig
import com.simplemobiletools.commons.extensions.toInt
import java.text.SimpleDateFormat
import java.util.*

class AlarmService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {

        val dateLong = System.currentTimeMillis()
        val currentDate = SimpleDateFormat("d", Locale.getDefault()).format(dateLong)


        val cur = currentDate.toInt()
        packageManager.setComponentEnabledSetting(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                "com.daily.events.calender.LauncherAlias$cur"
            ),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )

        for (i in 1..31) {
            if (i != currentDate.toInt()) {
                packageManager.setComponentEnabledSetting(
                    ComponentName(
                        BuildConfig.APPLICATION_ID,
                        "com.daily.events.calender.LauncherAlias$i"
                    ),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    companion object {
        const val ANDROID_CHANNEL_ID = "com.daily.events.calender"

        const val JOB_ID = 1000
        fun enqueueWork(context: Context?, work: Intent?) {
            enqueueWork(
                context!!,
                AlarmService::class.java, JOB_ID, work!!
            )
        }
    }
}

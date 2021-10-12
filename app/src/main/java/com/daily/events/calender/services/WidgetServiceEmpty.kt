package com.daily.events.calender.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.daily.events.calender.Adapter.EventListWidgetAdapterEmpty

class WidgetServiceEmpty : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapterEmpty(applicationContext)
}

package com.daily.events.calender.dialogs

import androidx.appcompat.app.AlertDialog
import com.daily.events.calender.Activity.SimpleActivity
import com.daily.events.calender.Adapter.FilterEventTypeAdapter
import com.daily.events.calender.Extensions.config
import com.daily.events.calender.Extensions.eventsHelper
import com.daily.events.calender.R
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_filter_event_types.view.*

class FilterEventTypesDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    private lateinit var dialog: AlertDialog
    private val view = activity.layoutInflater.inflate(R.layout.dialog_filter_event_types, null)

    init {
        activity.eventsHelper.getEventTypes(activity, false) {
            val displayEventTypes = activity.config.displayEventTypes
            view.filter_event_types_list.adapter =
                FilterEventTypeAdapter(activity, it, displayEventTypes)

            dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmEventTypes() }
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
        }
    }

    private fun confirmEventTypes() {
        val selectedItems =
            (view.filter_event_types_list.adapter as FilterEventTypeAdapter).getSelectedItemsList()
                .map { it.toString() }.toHashSet()
        if (activity.config.displayEventTypes != selectedItems) {
            activity.config.displayEventTypes = selectedItems
            callback()
        }
        dialog.dismiss()
    }
}

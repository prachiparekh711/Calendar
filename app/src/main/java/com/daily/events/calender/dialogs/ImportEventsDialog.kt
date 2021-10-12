package com.daily.events.calender.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.daily.events.calender.Activity.SimpleActivity
import com.daily.events.calender.Extensions.config
import com.daily.events.calender.Extensions.eventTypesDB
import com.daily.events.calender.Extensions.eventsHelper
import com.daily.events.calender.R
import com.daily.events.calender.helpers.IcsImporter
import com.daily.events.calender.helpers.IcsImporter.ImportResult.*
import com.daily.events.calender.helpers.REGULAR_EVENT_TYPE_ID
import com.simplemobiletools.commons.extensions.getCornerRadius
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.dialog_import_events.view.*

class ImportEventsDialog(
    val activity: SimpleActivity,
    val path: String,
    val callback: (refreshView: Boolean) -> Unit
) {
    var currEventTypeId = REGULAR_EVENT_TYPE_ID
    var currEventTypeCalDAVCalendarId = 0
    val config = activity.config

    init {
        ensureBackgroundThread {
            if (activity.eventTypesDB.getEventTypeWithId(config.lastUsedLocalEventTypeId) == null) {
                config.lastUsedLocalEventTypeId = REGULAR_EVENT_TYPE_ID
            }

            val isLastCaldavCalendarOK = config.caldavSync && config.getSyncedCalendarIdsAsList()
                .contains(config.lastUsedCaldavCalendarId)
            currEventTypeId = if (isLastCaldavCalendarOK) {
                val lastUsedCalDAVCalendar =
                    activity.eventsHelper.getEventTypeWithCalDAVCalendarId(config.lastUsedCaldavCalendarId)
                if (lastUsedCalDAVCalendar != null) {
                    currEventTypeCalDAVCalendarId = config.lastUsedCaldavCalendarId
                    lastUsedCalDAVCalendar.id!!
                } else {
                    REGULAR_EVENT_TYPE_ID
                }
            } else {
                config.lastUsedLocalEventTypeId
            }

            activity.runOnUiThread {
                initDialog()
            }
        }
    }

    private fun initDialog() {
        val view = (activity.layoutInflater.inflate(
            R.layout.dialog_import_events,
            null
        ) as ViewGroup).apply {
            updateEventType(this)
            import_event_type_holder.setOnClickListener {
                SelectEventTypeDialog(activity, currEventTypeId, true, true, false, true) {
                    currEventTypeId = it.id!!
                    currEventTypeCalDAVCalendarId = it.caldavCalendarId

                    config.lastUsedLocalEventTypeId = it.id!!
                    config.lastUsedCaldavCalendarId = it.caldavCalendarId

                    updateEventType(this)
                }
            }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.import_events) {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null)
                        activity.toast(R.string.importing)
                        ensureBackgroundThread {
                            val overrideFileEventTypes = view.import_events_checkbox.isChecked
                            val result = IcsImporter(activity).importEvents(
                                path,
                                currEventTypeId,
                                currEventTypeCalDAVCalendarId,
                                overrideFileEventTypes
                            )
                            handleParseResult(result)
                            dismiss()
                        }
                    }
                }
            }
    }

    private fun updateEventType(view: ViewGroup) {
        ensureBackgroundThread {
            val eventType = activity.eventTypesDB.getEventTypeWithId(currEventTypeId)
            activity.runOnUiThread {
                view.import_event_type_title.text = eventType!!.getDisplayTitle()
                view.import_event_type_color.setFillWithStroke(
                    eventType.color,
                    activity.config.backgroundColor,
                    activity.getCornerRadius()
                )
            }
        }
    }

    private fun handleParseResult(result: IcsImporter.ImportResult) {
        activity.toast(
            when (result) {
                IMPORT_NOTHING_NEW -> R.string.no_new_items
                IMPORT_OK -> R.string.importing_successful
                IMPORT_PARTIAL -> R.string.importing_some_entries_failed
                else -> R.string.no_items_found
            }
        )
        callback(result != IMPORT_FAIL)
    }
}

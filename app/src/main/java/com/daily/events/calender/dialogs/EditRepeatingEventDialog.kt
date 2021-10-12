package com.daily.events.calender.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.daily.events.calender.R
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_edit_repeating_event.view.*

class EditRepeatingEventDialog(
    val activity: AppCompatActivity,
    val callback: (allOccurrences: Int) -> Unit
) {
    var dialog: AlertDialog

    init {
        val view = (activity.layoutInflater.inflate(
            R.layout.dialog_edit_repeating_event,
            null
        ) as ViewGroup).apply {
            edit_repeating_event_one_only.setOnClickListener { sendResult(0) }
            edit_repeating_event_this_and_future_occurences.setOnClickListener { sendResult(1) }
            edit_repeating_event_all_occurrences.setOnClickListener { sendResult(2) }
        }

        dialog = AlertDialog.Builder(activity)
            .create().apply {
                activity.setupDialogStuff(view, this) {
                    hideKeyboard()
                }
            }
    }

    private fun sendResult(allOccurrences: Int) {
        callback(allOccurrences)
        dialog.dismiss()
    }
}

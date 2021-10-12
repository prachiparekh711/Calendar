package com.daily.events.calender.Fragment

import android.Manifest
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.airbnb.lottie.LottieAnimationView
import com.daily.events.calender.Activity.MainActivity
import com.daily.events.calender.Activity.PolicyActivity
import com.daily.events.calender.BuildConfig
import com.daily.events.calender.Extensions.calDAVHelper
import com.daily.events.calender.Extensions.config
import com.daily.events.calender.Extensions.eventTypesDB
import com.daily.events.calender.R
import com.daily.events.calender.databinding.FragmentSettingBinding
import com.daily.events.calender.services.AlarmReceiver
import com.simplemobiletools.commons.activities.BaseSimpleActivity.Companion.perms
import com.simplemobiletools.commons.helpers.*
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingFragment : Fragment() {

    var actionOnPermission: ((granted: Boolean) -> Unit)? = null
    var isAskingPermissions = false
    var useDynamicTheme = true
    var checkedDocumentPath = ""

    private val GENERIC_PERM_HANDLER = 100

    var fragmentSetting: FragmentSettingBinding? = null

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onResume() {
        super.onResume()
        setupCaldavSync()
    }

    private fun setupCaldavSync() {
        fragmentSetting?.imgSync?.setOnClickListener {
            val isGranted = EasyPermissions.hasPermissions(requireContext(), *perms)
            if (isGranted) {
                if (!requireContext().config.caldavSync) {
                    toggleCaldavSync(true)
                } else {
                    toggleCaldavSync(false)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentSetting =
            DataBindingUtil.inflate(inflater, R.layout.fragment_setting, container, false)

        MainActivity.mainBinding?.dateTitleTV?.text = resources.getString(R.string.settings)

        fragmentSetting?.icAddHolidays?.setOnClickListener {
            val lbm = LocalBroadcastManager.getInstance(requireContext())
            val localIn = Intent("ADD_HOLIDAYS")
            lbm.sendBroadcast(localIn)
        }

        fragmentSetting?.icAddBirthday?.setOnClickListener {
            val lbm = LocalBroadcastManager.getInstance(requireContext())
            val localIn = Intent("ADD_BIRTHDAY")
            lbm.sendBroadcast(localIn)
        }

        fragmentSetting?.icAddAnniversary?.setOnClickListener {
            val lbm = LocalBroadcastManager.getInstance(requireContext())
            val localIn = Intent("ADD_ANNIVERSARY")
            lbm.sendBroadcast(localIn)
        }

        fragmentSetting?.icCustomIcons?.setOnClickListener {
            requireActivity().packageManager.setComponentEnabledSetting(
                ComponentName(
                    BuildConfig.APPLICATION_ID,
                    "com.daily.events.calender.DefaultLauncher"
                ),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )

            val dateLong = System.currentTimeMillis()
            val currentDate = SimpleDateFormat("d", Locale.getDefault()).format(dateLong)

            val cur = currentDate.toInt()
            requireActivity().packageManager.setComponentEnabledSetting(
                ComponentName(
                    BuildConfig.APPLICATION_ID,
                    "com.daily.events.calender.LauncherAlias$cur"
                ),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )

            val calendar = Calendar.getInstance()
            AlarmReceiver().setRepeatAlarm(requireContext(), 1001, calendar)
            requireActivity().runOnUiThread {
                Toast.makeText(
                    MainActivity.activity,
                    "Custom icon theme applied. Please relaunch app.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        fragmentSetting?.privacyPolicy?.setOnClickListener {
            val intent = Intent(activity, PolicyActivity::class.java)
            startActivity(intent)
        }

        fragmentSetting?.rateUs?.setOnClickListener {
            showRateDialog()
        }

        fragmentSetting?.feedback?.setOnClickListener {
            sendFeedback()
        }

        fragmentSetting?.shareApp?.setOnClickListener {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Calendar")
                var shareMessage = "\nLet me recommend you this application\n\n"
                shareMessage =
                    """
        ${shareMessage}https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}
        
        
        """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                //e.toString();
            }
        }


        return fragmentSetting?.root
    }

    private fun showRateDialog() {
        val dialog = Dialog(requireActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialige_rate_us)
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))
        dialog.setCanceledOnTouchOutside(true)
        val animationView = dialog.findViewById(R.id.animationView) as LottieAnimationView
        animationView.animate()
        val yesBtn = dialog.findViewById(R.id.yes) as TextView
        val noBtn = dialog.findViewById(R.id.no) as TextView
        yesBtn.setOnClickListener {
            val appPackageName =
                requireActivity().packageName

            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$appPackageName")
                    )
                )
            } catch (anfe: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                    )
                )
            }
            dialog.dismiss()
        }
        noBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()

    }

    fun sendFeedback() {
        val emailIntent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:" + "bitbotdevelopers@gmail.com")
        )
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback")
        try {
            startActivity(Intent.createChooser(emailIntent, "Send email via..."))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                requireActivity().applicationContext,
                "There are no email clients installed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleCaldavSync(enable: Boolean) {
        if (enable) {
            val lbm = LocalBroadcastManager.getInstance(requireContext())
            val localIn = Intent("OPEN_ACCOUNT_SYNC")
            lbm.sendBroadcast(localIn)
        } else {
            requireContext().config.caldavSync = false

            ensureBackgroundThread {
                requireContext().config.getSyncedCalendarIdsAsList().forEach {
                    requireContext().calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                }
                requireContext().eventTypesDB.deleteEventTypesWithCalendarId(requireContext().config.getSyncedCalendarIdsAsList())
                updateDefaultEventTypeText()
            }
        }
    }

    private fun updateDefaultEventTypeText() {
        if (requireContext().config.defaultEventTypeId == -1L) {

        } else {
            ensureBackgroundThread {
                val eventType =
                    requireContext().eventTypesDB.getEventTypeWithId(requireContext().config.defaultEventTypeId)
                if (eventType != null) {
                    requireContext().config.lastUsedCaldavCalendarId = eventType.caldavCalendarId
                } else {
                    requireContext().config.defaultEventTypeId = -1
                    updateDefaultEventTypeText()
                }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasPermission(permissionId)) {
            callback(true)
        } else {
            isAskingPermissions = true
            actionOnPermission = callback
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(getPermissionString(permissionId)),
                GENERIC_PERM_HANDLER
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        isAskingPermissions = false
        if (requestCode == GENERIC_PERM_HANDLER && grantResults.isNotEmpty()) {
            actionOnPermission?.invoke(grantResults[0] == 0)
        }
    }

    fun hasPermission(permId: Int) = ContextCompat.checkSelfPermission(
        requireContext(),
        getPermissionString(permId)
    ) == PackageManager.PERMISSION_GRANTED

    fun getPermissionString(id: Int) = when (id) {
        PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
        PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
        PERMISSION_CAMERA -> Manifest.permission.CAMERA
        PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
        PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
        PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
        PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
        PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
        PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
        PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
        PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
        PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
        PERMISSION_READ_SMS -> Manifest.permission.READ_SMS
        PERMISSION_SEND_SMS -> Manifest.permission.SEND_SMS
        PERMISSION_READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
        else -> ""
    }


}
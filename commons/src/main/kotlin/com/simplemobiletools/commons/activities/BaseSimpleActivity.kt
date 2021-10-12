package com.simplemobiletools.commons.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.util.Pair
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FileConflictDialog
import com.simplemobiletools.commons.dialogs.WritePermissionDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.interfaces.CopyMoveListener
import com.simplemobiletools.commons.models.FileDirItem
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.OutputStream
import java.util.*
import java.util.regex.Pattern

abstract class BaseSimpleActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    var copyMoveCallback: ((destinationPath: String) -> Unit)? = null
    var actionOnPermission: ((granted: Boolean) -> Unit)? = null
    var isAskingPermissions = false
    var useDynamicTheme = true
    var showTransparentTop = false
    var checkedDocumentPath = ""
    var configItemsToExport = LinkedHashMap<String, Any>()

    private val GENERIC_PERM_HANDLER = 100

    companion object {

        const val RC_READ_EXTERNAL_STORAGE = 123
        var perms = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CONTACTS,
        )
        var funAfterSAFPermission: ((success: Boolean) -> Unit)? = null
        var permissionApply: Boolean = false
    }

    abstract fun permissionGranted()
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        readExternalStorage()
        ActivityCompat.requestPermissions(
            this,
            perms, 1
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String?>?) {
//        Log.e("hasPermissions", "true")
        permissionGranted()
    }

    override fun onPermissionsDenied(requestCode: Int, perms1: List<String?>?) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms1!!)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            EasyPermissions.requestPermissions(
                this@BaseSimpleActivity,
                getString(R.string.permission_str),
                RC_READ_EXTERNAL_STORAGE,
                *perms
            )
        }
    }

    @AfterPermissionGranted(RC_READ_EXTERNAL_STORAGE)
    private fun readExternalStorage() {
        val isGranted = EasyPermissions.hasPermissions(this, *perms)
        if (isGranted) {

            permissionGranted()
        } else {
            EasyPermissions.requestPermissions(
                this@BaseSimpleActivity, getString(R.string.permission_str),
                RC_READ_EXTERNAL_STORAGE, *perms
            )
        }
    }

    abstract fun getAppIconIDs(): ArrayList<Int>

    abstract fun getAppLauncherName(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        if (useDynamicTheme) {
            setTheme(getThemeId(showTransparentTop = showTransparentTop))
        }

        super.onCreate(savedInstanceState)

        if (!packageName.startsWith("com.daily.events.calender", true)) {
            if ((0..50).random() == 10 || baseConfig.appRunCount % 100 == 0) {
//                val label =
//                    "You are using a fake version of the app. For your own safety download the original one from www.simplemobiletools.com. Thanks"
//                ConfirmationDialog(this, label, positive = R.string.ok, negative = 0) {
//                    launchViewIntent("https://play.google.com/store/apps/dev?id=9070296388022589266")
//                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (useDynamicTheme) {
            setTheme(getThemeId(showTransparentTop = showTransparentTop))
            updateBackgroundColor()
        }

        if (showTransparentTop) {
            window.statusBarColor = Color.TRANSPARENT
        } else {
            updateActionbarColor()
        }

//        updateRecentsAppIcon()
        updateNavigationBarColor()

    }

    override fun onStop() {
        super.onStop()
        actionOnPermission = null
    }

    override fun onDestroy() {
        super.onDestroy()
        funAfterSAFPermission = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun attachBaseContext(newBase: Context) {
        if (newBase.baseConfig.useEnglish) {
            super.attachBaseContext(MyContextWrapper(newBase).wrap(newBase, "en"))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    fun updateBackgroundColor(color: Int = baseConfig.backgroundColor) {
        window.decorView.setBackgroundColor(color)
    }

    fun updateActionbarColor(color: Int = baseConfig.primaryColor) {
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        updateActionBarTitle(supportActionBar?.title.toString(), color)
        updateStatusbarColor(color)
        setTaskDescription(ActivityManager.TaskDescription(null, null, color))
    }

    fun updateStatusbarColor(color: Int) {
        window.statusBarColor = color.darkenColor()

        if (isMarshmallowPlus()) {
            if (color.getContrastColor() == 0xFF333333.toInt()) {
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility.addBit(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            } else {
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility.removeBit(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
        }
    }

    fun updateNavigationBarColor(color: Int = baseConfig.navigationBarColor) {
        if (baseConfig.navigationBarColor != INVALID_NAVIGATION_BAR_COLOR) {
            try {
                val colorToUse = if (color == -2) -1 else color
                window.navigationBarColor = resources.getColor(R.color.theme_color)

                if (isOreoPlus()) {
                    if (color.getContrastColor() == 0xFF333333.toInt()) {
                        window.decorView.systemUiVisibility =
                            window.decorView.systemUiVisibility.addBit(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                    } else {
                        window.decorView.systemUiVisibility =
                            window.decorView.systemUiVisibility.removeBit(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }

    fun updateRecentsAppIcon() {
        if (baseConfig.isUsingModifiedAppIcon) {
            val appIconIDs = getAppIconIDs()
            val currentAppIconColorIndex = getCurrentAppIconColorIndex()
            if (appIconIDs.size - 1 < currentAppIconColorIndex) {
                return
            }

            val recentsIcon =
                BitmapFactory.decodeResource(resources, appIconIDs[currentAppIconColorIndex])
            val title = getAppLauncherName()
            val color = resources.getColor(R.color.theme_color)

            val description = ActivityManager.TaskDescription(title, recentsIcon, color)
            setTaskDescription(description)
        }
    }

    fun updateMenuItemColors(
        menu: Menu?,
        useCrossAsBack: Boolean = false,
        baseColor: Int = resources.getColor(R.color.theme_color)
    ) {
        if (menu == null) {
            return
        }

        val color = resources.getColor(R.color.white)
        for (i in 0 until menu.size()) {
            try {
                menu.getItem(i)?.icon?.setTint(color)
            } catch (ignored: Exception) {
            }
        }

        val drawableId =
            if (useCrossAsBack) R.drawable.ic_cross_vector else R.drawable.ic_arrow_left_vector
        val icon =
            resources.getColoredDrawableWithColor(drawableId, resources.getColor(R.color.white))
        supportActionBar?.setHomeAsUpIndicator(icon)
    }

    private fun getCurrentAppIconColorIndex(): Int {
        val appIconColor = baseConfig.appIconColor
        getAppIconColors().forEachIndexed { index, color ->
            if (color == appIconColor) {
                return index
            }
        }
        return 0
    }

    fun setTranslucentNavigation() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        val partition = try {
            checkedDocumentPath.substring(9, 18)
        } catch (e: Exception) {
            ""
        }
        val sdOtgPattern = Pattern.compile(SD_OTG_SHORT)

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {

            // Do something after user returned from app settings screen, like showing a Toast.
            if (EasyPermissions.hasPermissions(this, *perms)) {
                permissionGranted()
            }
        }

        if (requestCode == RC_READ_EXTERNAL_STORAGE) {

            // Do something after user returned from app settings screen, like showing a Toast.
            if (EasyPermissions.hasPermissions(this, *perms)) {
                permissionGranted()
            } else {
                EasyPermissions.requestPermissions(
                    this, getString(R.string.permission_str),
                    RC_READ_EXTERNAL_STORAGE, *perms
                )
            }
        }

        if (requestCode == OPEN_DOCUMENT_TREE) {
            if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
                val isProperPartition = partition.isEmpty() || !sdOtgPattern.matcher(partition)
                    .matches() || (sdOtgPattern.matcher(partition)
                    .matches() && resultData.dataString!!.contains(partition))
                if (isProperSDFolder(resultData.data!!) && isProperPartition) {
                    if (resultData.dataString == baseConfig.OTGTreeUri) {
                        toast(R.string.sd_card_usb_same)
                        return
                    }

                    saveTreeUri(resultData)
                    funAfterSAFPermission?.invoke(true)
                    funAfterSAFPermission = null
                } else {
                    toast(R.string.wrong_root_selected)
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, requestCode)
                }
            } else {
                funAfterSAFPermission?.invoke(false)
            }
        } else if (requestCode == OPEN_DOCUMENT_TREE_OTG) {
            if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
                val isProperPartition = partition.isEmpty() || !sdOtgPattern.matcher(partition)
                    .matches() || (sdOtgPattern.matcher(partition)
                    .matches() && resultData.dataString!!.contains(partition))
                if (isProperOTGFolder(resultData.data!!) && isProperPartition) {
                    if (resultData.dataString == baseConfig.treeUri) {
                        funAfterSAFPermission?.invoke(false)
                        permissionApply = false
                        toast(R.string.sd_card_usb_same)
                        return
                    }
                    baseConfig.OTGTreeUri = resultData.dataString!!
                    baseConfig.OTGPartition =
                        baseConfig.OTGTreeUri.removeSuffix("%3A").substringAfterLast('/')
                            .trimEnd('/')
                    updateOTGPathFromPartition()

                    val takeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    applicationContext.contentResolver.takePersistableUriPermission(
                        resultData.data!!,
                        takeFlags
                    )

                    funAfterSAFPermission?.invoke(true)
                    funAfterSAFPermission = null
                    permissionApply = true
                } else {
                    toast(R.string.wrong_root_selected_usb)
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, requestCode)
                }
            } else {
                funAfterSAFPermission?.invoke(false)
            }
        } else if (requestCode == SELECT_EXPORT_SETTINGS_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportSettingsTo(outputStream, configItemsToExport)
        }

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            if (EasyPermissions.hasPermissions(this, *perms)) {
                permissionGranted()
            }
        }

        if (requestCode == RC_READ_EXTERNAL_STORAGE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            if (EasyPermissions.hasPermissions(this, *perms)) {
                permissionGranted()
            }
        }
    }

    private fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        baseConfig.treeUri = treeUri.toString()

        val takeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(treeUri!!, takeFlags)
    }

    private fun isProperSDFolder(uri: Uri) =
        isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)

    private fun isProperOTGFolder(uri: Uri) =
        isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)

    private fun isRootUri(uri: Uri) = DocumentsContract.getTreeDocumentId(uri).endsWith(":")

    private fun isInternalStorage(uri: Uri) =
        isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri)
            .contains("primary")

    private fun isExternalStorageDocument(uri: Uri) =
        "com.android.externalstorage.documents" == uri.authority


    @RequiresApi(Build.VERSION_CODES.O)
    fun launchCustomizeNotificationsIntent() {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(this)
        }
    }

    // synchronous return value determines only if we are showing the SAF dialog, callback result tells if the SD or OTG permission has been granted
    fun handleSAFDialog(path: String, callback: (success: Boolean) -> Unit): Boolean {
        return if (!packageName.startsWith("com.simplemobiletools")) {
            callback(true)
            false
        } else if (isShowingSAFDialog(path) || isShowingOTGDialog(path)) {
            funAfterSAFPermission = callback
            true
        } else {
            callback(true)
            false
        }
    }

    fun handleOTGPermission(callback: (success: Boolean) -> Unit) {
        if (baseConfig.OTGTreeUri.isNotEmpty()) {
            callback(true)
            return
        }

        funAfterSAFPermission = callback
        WritePermissionDialog(this, true) {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                try {
                    startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                    return@apply
                } catch (e: Exception) {
                    type = "*/*"
                }

                try {
                    startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                } catch (e: Exception) {
                    toast(R.string.unknown_error_occurred)
                }
            }
        }
    }

    fun copyMoveFilesTo(
        fileDirItems: ArrayList<FileDirItem>,
        source: String,
        destination: String,
        isCopyOperation: Boolean,
        copyPhotoVideoOnly: Boolean,
        copyHidden: Boolean,
        callback: (destinationPath: String) -> Unit
    ) {
        if (source == destination) {
            toast(R.string.source_and_destination_same)
            return
        }

        if (!getDoesFilePathExist(destination)) {
            toast(R.string.invalid_destination)
            return
        }

        handleSAFDialog(destination) {
            if (!it) {
                copyMoveListener.copyFailed()
                return@handleSAFDialog
            }

            copyMoveCallback = callback
            var fileCountToCopy = fileDirItems.size
            if (isCopyOperation) {
                startCopyMove(
                    fileDirItems,
                    destination,
                    isCopyOperation,
                    copyPhotoVideoOnly,
                    copyHidden
                )
            } else {
                if (isPathOnOTG(source) || isPathOnOTG(destination) || isPathOnSD(source) || isPathOnSD(
                        destination
                    ) || fileDirItems.first().isDirectory
                ) {
                    handleSAFDialog(source) {
                        if (it) {
                            startCopyMove(
                                fileDirItems,
                                destination,
                                isCopyOperation,
                                copyPhotoVideoOnly,
                                copyHidden
                            )
                        }
                    }
                } else {
                    try {
                        checkConflicts(fileDirItems, destination, 0, LinkedHashMap()) {
                            toast(R.string.moving)
                            ensureBackgroundThread {
                                val updatedPaths = ArrayList<String>(fileDirItems.size)
                                val destinationFolder = File(destination)
                                for (oldFileDirItem in fileDirItems) {
                                    var newFile = File(destinationFolder, oldFileDirItem.name)
                                    if (newFile.exists()) {
                                        when {
                                            getConflictResolution(
                                                it,
                                                newFile.absolutePath
                                            ) == CONFLICT_SKIP -> fileCountToCopy--
                                            getConflictResolution(
                                                it,
                                                newFile.absolutePath
                                            ) == CONFLICT_KEEP_BOTH -> newFile =
                                                getAlternativeFile(newFile)
                                            else ->
                                                // this file is guaranteed to be on the internal storage, so just delete it this way
                                                newFile.delete()
                                        }
                                    }

                                    if (!newFile.exists() && File(oldFileDirItem.path).renameTo(
                                            newFile
                                        )
                                    ) {
                                        if (!baseConfig.keepLastModified) {
                                            newFile.setLastModified(System.currentTimeMillis())
                                        }
                                        updatedPaths.add(newFile.absolutePath)
                                        deleteFromMediaStore(oldFileDirItem.path)
                                    }
                                }

                                runOnUiThread {
                                    if (updatedPaths.isEmpty()) {
                                        copyMoveListener.copySucceeded(
                                            false,
                                            fileCountToCopy == 0,
                                            destination
                                        )
                                    } else {
                                        copyMoveListener.copySucceeded(
                                            false,
                                            fileCountToCopy <= updatedPaths.size,
                                            destination
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }
        }
    }

    fun getAlternativeFile(file: File): File {
        var fileIndex = 1
        var newFile: File?
        do {
            val newName =
                String.format("%s(%d).%s", file.nameWithoutExtension, fileIndex, file.extension)
            newFile = File(file.parent, newName)
            fileIndex++
        } while (getDoesFilePathExist(newFile!!.absolutePath))
        return newFile
    }

    private fun startCopyMove(
        files: ArrayList<FileDirItem>,
        destinationPath: String,
        isCopyOperation: Boolean,
        copyPhotoVideoOnly: Boolean,
        copyHidden: Boolean
    ) {
        val availableSpace = destinationPath.getAvailableStorageB()
        val sumToCopy = files.sumByLong { it.getProperSize(applicationContext, copyHidden) }
        if (availableSpace == -1L || sumToCopy < availableSpace) {
            checkConflicts(files, destinationPath, 0, LinkedHashMap()) {
                toast(if (isCopyOperation) R.string.copying else R.string.moving)
                val pair = Pair(files, destinationPath)
                CopyMoveTask(
                    this,
                    isCopyOperation,
                    copyPhotoVideoOnly,
                    it,
                    copyMoveListener,
                    copyHidden
                ).execute(pair)
            }
        } else {
            val text = String.format(
                getString(R.string.no_space),
                sumToCopy.formatSize(),
                availableSpace.formatSize()
            )
            toast(text, Toast.LENGTH_LONG)
        }
    }

    fun checkConflicts(
        files: ArrayList<FileDirItem>,
        destinationPath: String,
        index: Int,
        conflictResolutions: LinkedHashMap<String, Int>,
        callback: (resolutions: LinkedHashMap<String, Int>) -> Unit
    ) {
        if (index == files.size) {
            callback(conflictResolutions)
            return
        }

        val file = files[index]
        val newFileDirItem =
            FileDirItem("$destinationPath/${file.name}", file.name, file.isDirectory)
        if (getDoesFilePathExist(newFileDirItem.path)) {
            FileConflictDialog(this, newFileDirItem, files.size > 1) { resolution, applyForAll ->
                if (applyForAll) {
                    conflictResolutions.clear()
                    conflictResolutions[""] = resolution
                    checkConflicts(
                        files,
                        destinationPath,
                        files.size,
                        conflictResolutions,
                        callback
                    )
                } else {
                    conflictResolutions[newFileDirItem.path] = resolution
                    checkConflicts(files, destinationPath, index + 1, conflictResolutions, callback)
                }
            }
        } else {
            checkConflicts(files, destinationPath, index + 1, conflictResolutions, callback)
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
                this,
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
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
//        isAskingPermissions = false
//        if (requestCode == GENERIC_PERM_HANDLER && grantResults.isNotEmpty()) {
//            actionOnPermission?.invoke(grantResults[0] == 0)
//        }
    }

    val copyMoveListener = object : CopyMoveListener {
        override fun copySucceeded(copyOnly: Boolean, copiedAll: Boolean, destinationPath: String) {
            if (copyOnly) {
                toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
            } else {
                toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
            }

            copyMoveCallback?.invoke(destinationPath)
            copyMoveCallback = null
        }

        override fun copyFailed() {
            toast(R.string.copy_move_failed)
            copyMoveCallback = null
        }
    }

    fun checkAppOnSDCard() {
        if (!baseConfig.wasAppOnSDShown && isAppInstalledOnSDCard()) {
            baseConfig.wasAppOnSDShown = true
            ConfirmationDialog(this, "", R.string.app_on_sd_card, R.string.ok, 0) {}
        }
    }


    private fun exportSettingsTo(
        outputStream: OutputStream?,
        configItems: LinkedHashMap<String, Any>
    ) {
        if (outputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ensureBackgroundThread {
            outputStream.bufferedWriter().use { out ->
                for ((key, value) in configItems) {
                    out.writeLn("$key=$value")
                }
            }

            toast(R.string.settings_exported_successfully)
        }
    }

    private fun getExportSettingsFilename(): String {
        var defaultFilename = baseConfig.lastExportedSettingsFile
        if (defaultFilename.isEmpty()) {
            val appName = baseConfig.appId.removeSuffix(".debug").removeSuffix(".pro")
                .removePrefix("com.daily.events.calender")
            defaultFilename = "$appName-settings.txt"
        }

        return defaultFilename
    }

    @SuppressLint("InlinedApi")
    protected fun launchSetDefaultDialerIntent() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(
                    RoleManager.ROLE_DIALER
                )
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
            }
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                packageName
            ).apply {
                try {
                    startActivityForResult(this, REQUEST_CODE_SET_DEFAULT_DIALER)
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.no_app_found)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }
}

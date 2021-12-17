package com.ordergoapp.utils


import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.ordergoapp.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates


class Utils {
    companion object {
        var loadingDialog: Dialog? = null
        val INTENT_TABLE_ID: String = "tableId"

        fun showLoading(state: Boolean, context: Context) {
            if (state)
                showCustomLoadingDialog(context);
            else
                hideCustomLoadingDialog();

        }

        fun showCustomLoadingDialog(context: Context) {
            Log.e("Observe", "Observe $loadingDialog")
            if (loadingDialog == null) {
                loadingDialog = Dialog(context)
                loadingDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
                loadingDialog?.setCancelable(false)
                loadingDialog?.setContentView(R.layout.dialog_loading_bar)
                loadingDialog?.getWindow()?.setBackgroundDrawable(
                    ColorDrawable(Color.TRANSPARENT)
                )
                loadingDialog?.show()
            }
        }

        fun hideCustomLoadingDialog() {
            if (loadingDialog != null) {
                loadingDialog?.dismiss();
                loadingDialog = null;
            }

        }

        fun getDateTimeDiff(date1: String, date2: String): Long { //2021-10-25T19:26:33+01:00
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK)
            //dateFormat.timeZone = TimeZone.getTimeZone("Europe/")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            val diff =
                dateFormat.parse(date1).time - dateFormat.parse(date2).time

            return diff
        }

        fun getDateTimeDiff(date: String): Long { //2021-10-25T19:26:33+01:00
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.UK)// 'Z'
            //dateFormat.timeZone = TimeZone.getTimeZone("Europe/")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            val diff =
                Date().time - dateFormat.parse(date).time

            return diff
        }

        fun hideKeyboardFrom(context: Context, view: View) {
            val imm: InputMethodManager =
                context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun getVersionName(context:Context): String {
            //return BuildConfig.VERSION_NAME
            try {
                return  context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                return ""
            }
        }

        fun updateApp(context: Context) {
            val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
            val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo
            // Checks that the platform will allow the specified type of update.
            appUpdateInfoTask.addOnSuccessListener { result: AppUpdateInfo ->
                if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {

                    val alertDialogBuilder =
                        AlertDialog.Builder(context)
                    alertDialogBuilder.setTitle(R.string.app_name)
                    alertDialogBuilder.setIcon(R.drawable.play_logo_icon)
                    alertDialogBuilder.setCancelable(false)
                    alertDialogBuilder.setMessage(context.getString(R.string.app_name) + " must update to the latest version for a seamless & enhanced performance of the app.")
                    alertDialogBuilder.setPositiveButton(
                        "Update"
                    ) { _, _ ->
                        try {
                            context.startActivity(
                                Intent(
                                    "android.intent.action.VIEW",
                                    Uri.parse("market://details?id=" + context.packageName)
                                )
                            )
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(
                                    "android.intent.action.VIEW",
                                    Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName)
                                )
                            )
                        }
                    }

                    alertDialogBuilder.show()
                } else {
                }
            }
                .addOnFailureListener {
                    val alertDialogBuilder =
                        AlertDialog.Builder(context)
                    alertDialogBuilder.setTitle(R.string.app_name)
                    alertDialogBuilder.setIcon(R.drawable.play_logo_icon)
                    alertDialogBuilder.setCancelable(false)
                    alertDialogBuilder.setMessage(context.getString(R.string.app_name) + " must update to the latest version for a seamless & enhanced performance of the app.")
                    alertDialogBuilder.setPositiveButton(
                        "Update"
                    ) { _, _ ->
                        try {
                            context.startActivity(
                                Intent(
                                    "android.intent.action.VIEW",
                                    Uri.parse("market://details?id=" + context.packageName)
                                )
                            )
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(
                                    "android.intent.action.VIEW",
                                    Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName)
                                )
                            )
                        }
                    }

                    alertDialogBuilder.show()
                    //Log.d("UpdateAppFailure", it.message.toString())
                    //Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }
        }
    }

}

object Variables {
    var placedOrdersHasPlacedItems: Int = 0
    var placedOrdersCount = 0
    val networkNotifyListeners = ArrayList<InterfaceNetworkNotify>()

    var isNetworkConnected: Boolean by Delegates.observable(false) { property, oldValue, newValue ->
        Log.i("Network connectivity", "$newValue")
        networkNotifyListeners.forEach {
            it.networkChange(oldValue, newValue)
        }
    }

    interface InterfaceNetworkNotify {
        fun networkChange(old: Boolean, new: Boolean)
    }

    interface InterfaceIsCompletedTab {
        fun completedTabChanges(old: Boolean, new: Boolean)
    }

    var completedTabListener: InterfaceIsCompletedTab? = null
    var isCompletedTabOpened: Boolean by Delegates.observable(false) { prop, old, new ->

        completedTabListener?.completedTabChanges(old, new)

    }
}
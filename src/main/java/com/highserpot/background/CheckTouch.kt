package com.highserpot.background

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.text.Html
import android.view.accessibility.AccessibilityManager
import android.widget.Toast

var clickable = false
class CheckTouch(val context: Context) {
    lateinit var sharedPref: SharedPreferences

    val am: AccessibilityManager by lazy {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }



    fun isAccessServiceEnabled(context: Context): Boolean {
        val prefString =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        return prefString.contains(context.packageName + "/com.highserpot.background.service.TouchService")
    }

    fun checkAccessibilityPermissions(): Boolean {
        if (am.isEnabled && isAccessServiceEnabled(context)) {
            clickable = true
            return true
        }else{
            clickable = false
            Toast.makeText(
                context,
                context.getString(R.string.need_click),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }


    }

    fun setAccessibilityPermissions() {
        val gsDialog: AlertDialog.Builder = AlertDialog.Builder(context)


        val txt_t = context.getString(R.string.gsDialog_title)
        val txt_a = context.getString(R.string.gsDialog_action)
        val txt_c = String.format(context.getString(R.string.gsDialog_context), context.getString(R.string.app_name))

        gsDialog.setTitle(txt_t)
        gsDialog.setMessage(txt_c)

        gsDialog.setPositiveButton(txt_a,
            DialogInterface.OnClickListener { dialog, which ->
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@OnClickListener
            }).create().show()
    }


    fun alert_dialog() {
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setPositiveButton(
            R.string.agree
        ) { dialog, id ->
            dialog.dismiss()
            sharedPref =
                context.getSharedPreferences(
                    context.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )

            val editor = sharedPref.edit()
            editor.putBoolean(context.getString(R.string.preference_file_key), true)
            editor.apply()

        }
        builder.setNegativeButton(
            R.string.not_agree
        ) { dialog, id ->
            val at = context as Activity
            at.moveTaskToBack(true)
            at.finishAndRemoveTask()
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        builder.setMessage(R.string.dialog_message)
            .setTitle(R.string.dialog_title)

        builder.create().show()
    }

    fun checkFirstRun() {
        sharedPref =
            context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

//        val editor = sharedPref.edit()
//        editor.putBoolean(context.getString(R.string.preference_file_key), false)
//        editor.commit()

        if (!sharedPref.getBoolean(context.getString(R.string.preference_file_key), false)) {
            alert_dialog()
        }
    }
}
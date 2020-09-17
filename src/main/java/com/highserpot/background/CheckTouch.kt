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


class CheckTouch(val context: Context) {
    lateinit var sharedPref: SharedPreferences

    val am: AccessibilityManager by lazy {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    fun chk(): Boolean {

        if (!checkAccessibilityPermissions()) {
            setAccessibilityPermissions()
            checkFirstRun()
        } else {
            checkFirstRun()
            return true
        }
        return false
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
        if (am != null && am.isEnabled && isAccessServiceEnabled(context)) {
            return true
        }

        return false
    }

    fun setAccessibilityPermissions() {
        val gsDialog: AlertDialog.Builder = AlertDialog.Builder(context)

        gsDialog.setTitle(context.getString(R.string.gsDialog_title))

        val s1 = "확인을 눌러 설정페이지에서"
        val s2 = "<b>" + context.getString(R.string.app_name) + "</b> 찾아 켜주세요."
        val strMessage = Html.fromHtml("$s1<br>$s2")
        gsDialog.setMessage(strMessage)

        gsDialog.setPositiveButton("확인",
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
            editor.commit()

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
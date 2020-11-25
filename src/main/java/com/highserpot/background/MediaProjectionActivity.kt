package com.highserpot.background

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.highserpot.background.service.BackgroundService


open class MediaProjectionActivity : AppCompatActivity() {
    var mIntent: Intent? = null
    var REQ_CODE_OVERLAY_PERMISSION = 1

    fun new_bg(): Intent {
        if (mIntent == null) {
            mIntent = Intent(applicationContext, BackgroundService::class.java)
            return mIntent!!
        } else {
            return mIntent!!
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mediaprojection)

        val textView = findViewById<TextView>(R.id.manual)
        textView.text = Html.fromHtml(getString(R.string.manual))

        val action = intent.extras?.getString("action")
        mIntent = new_bg()
        if (action != null && action == "stop") {
            stopService(mIntent!!)
        }

    }

    fun service_stop_btn(view: View?) {
        stopService(mIntent)
    }

    fun service_start_btn(view: View?) {
        Log.d(
            "탑뷰",
            Settings.canDrawOverlays(applicationContext).toString()
        )
        if (Settings.canDrawOverlays(applicationContext)) {
            if (CheckTouch(this).chk()) {
                var mediaProjectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                var captureIntent: Intent = mediaProjectionManager.createScreenCaptureIntent()
                startActivityForResult(captureIntent, 1000)
            } else {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.accessibility_service_description),
                    Toast.LENGTH_SHORT
                ).show()

            }
        } else {
            onObtainingPermissionOverlayWindow()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            mIntent?.putExtra("resultCode", resultCode)
            mIntent?.putExtra("data", data)
            startService(mIntent)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    open fun onObtainingPermissionOverlayWindow() {
        Log.d(
            "탑뷰", "권한"

        )
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION)
    }

    fun danger_click(view: View?) {
        CheckTouch(this).alert_dialog()
    }

}
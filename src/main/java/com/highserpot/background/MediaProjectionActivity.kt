package com.highserpot.background

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.highserpot.background.service.BackgroundService
import com.highserpot.myad.Reward
import kotlinx.android.synthetic.main.activity_mediaprojection.*


open class MediaProjectionActivity : AppCompatActivity() {
    var msg_n = "접근성 권한이 필요해요."
    val msg_y = "접근성 권한을 얻었습니다.\n시작하기를 눌러주세요."
    var mIntent: Intent? = null
    var REQ_CODE_OVERLAY_PERMISSION = 1
    var reward: Reward = Reward(this)

    fun new_bg(): Intent {
        if (mIntent == null) {
            mIntent = Intent(applicationContext, BackgroundService::class.java)
            return mIntent!!
        } else {
            return mIntent!!
        }

    }

    override fun onResume() {
        super.onResume()
        // mIntent = new_bg()

        if (CheckTouch(this).chk()) {
            textView2.text = msg_y
        } else {
            textView2.text = msg_n
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mediaprojection)
        reward.ready()


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
        Log.e(
            "Settings.canDrawOverlays(applicationContext)",
            Settings.canDrawOverlays(applicationContext).toString()
        )
        if (Settings.canDrawOverlays(applicationContext)) {
            if (CheckTouch(this).chk()) {
                textView2.text = msg_y
                var mediaProjectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                var captureIntent: Intent = mediaProjectionManager.createScreenCaptureIntent()
                startActivityForResult(captureIntent, 1000)
            } else {
                textView2.text = msg_n
                Toast.makeText(applicationContext, msg_n, Toast.LENGTH_SHORT).show()

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
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION)
    }

    fun danger_click(view: View?) {
        CheckTouch(this).alert_dialog()
    }

    fun start_reward(): Boolean {
        reward.load()
        return reward.has_reward
    }
}
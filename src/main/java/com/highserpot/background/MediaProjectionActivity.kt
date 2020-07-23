package com.highserpot.background

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.highserpot.background.service.BackgroundService
import kotlinx.android.synthetic.main.activity_mediaprojection.*

open class MediaProjectionActivity : AppCompatActivity() {
    var msg_n = "접근성 권한이 필요해요."
    val msg_y = "접근성 권한을 얻었습니다.\n시작하기를 눌러주세요."
    var bg : BackgroundService ?= null
    var mIntent : Intent ?= null
    override fun onResume() {
        super.onResume()
        bg = BackgroundService()
        mIntent = Intent(applicationContext, BackgroundService::class.java)
        if (CheckTouch(this).chk()) {
            textView2.setText(msg_y)
        } else {
            textView2.setText(msg_n)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mediaprojection)
        //textView2.setText(msg)

        val action = intent.extras?.getString("action")
        if (action != null && action == "stop") {
            mIntent = Intent(applicationContext, BackgroundService::class.java)
            stopService(mIntent!!)
        }

    }

    fun service_stop_btn(view: View?) {
        stopService(mIntent)
    }

    fun service_start_btn(view: View?) {
        if (CheckTouch(this).chk()) {
            textView2.setText(msg_y)
            var mediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            var captureIntent: Intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, 1000)
        } else {
            textView2.setText(msg_n)
            Toast.makeText(applicationContext, msg_n, Toast.LENGTH_SHORT).show()

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            mIntent?.putExtra("resultCode",resultCode)
            mIntent?.putExtra("data",data)
            startService(mIntent)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
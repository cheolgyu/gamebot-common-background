package com.highserpot.background.service


import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.highserpot.background.R
import com.highserpot.background.Utils
import com.highserpot.background.notification.Noti
import com.highserpot.background.user_calickable
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import java.nio.ByteBuffer


class BackgroundService : BackgroundServiceMP() {
    companion object {
        var kakao_send_notify = false
    }

    var STORE_DIRECTORY: String? = null
    private val FOREGROUND_SERVICE_ID = 1000

    lateinit var utils: Utils
    lateinit var bsView: BackgroundServiceView
    lateinit var bsThread: BackgroundThread
    val TAG: String = this.javaClass.simpleName

    override fun onCreate() {
        Log.d(TAG, "=============start===================")
        utils = Utils(this.applicationContext)

        bsThread = BackgroundThread()
        bsThread!!.start()

        run_notify()
        ready_media()
        bsView = BackgroundServiceView(applicationContext)
        val filter = IntentFilter()
        filter.addAction(BCAST_CONFIGCHANGED)
        this.applicationContext.registerReceiver(mBroadcastReceiver, filter);
        BS_THREAD = true
    }

    @Throws(java.lang.Exception::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {

            my_resultCode = intent.getIntExtra("resultCode", 1000)
            my_data = intent.getParcelableExtra("data")

            createVirtualDisplay()
            createModel()
            detect_run!!.build(mWidth, mHeight)
            bsView.start()

        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    fun image_available_bitmap(): Bitmap? {
        var image = imageReader?.acquireNextImage()
        if (image != null) {
            val planes: Array<Image.Plane> = image.planes


            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride: Int = planes[0].pixelStride
            val rowStride: Int = planes[0].rowStride
            val rowPadding: Int = rowStride - pixelStride * mWidth

            val w: Int = mWidth + rowPadding / pixelStride

            var bitmap: Bitmap? = null
            bitmap = Bitmap.createBitmap(
                w,//+ rowPadding / pixelStride,
                mHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()
            return bitmap

        }

        return null
    }

    fun notify_kakao(string: String) {
        val title = string
        val defaultText = TextTemplate(
            text = title + """ """.trimIndent(),
            link = Link(
                webUrl = "https://play.google.com/store/apps/details?id=com.highserpot.sk2",
                mobileWebUrl = "https://play.google.com/store/apps/details?id=com.highserpot.sk2"
            )
        )
        val TAG = "카카오"
        TalkApiClient.instance.sendDefaultMemo(defaultText) { error ->
            if (error != null) {
                Log.e(TAG, "나에게 보내기 실패", error)
            } else {
                Log.i(TAG, "나에게 보내기 성공")
            }
        }
    }

    class ActionInfo {
        var x: Float = 0.0f
        var y: Float = 0.0f
        lateinit var action_type: String

        constructor(x: Float, y: Float, action_type: String) : this() {
            this.x = x
            this.y = y
            this.action_type = action_type
        }

        constructor()
    }

    fun tflite_bitmap(bitmap: Bitmap): ActionInfo? {

        val res = detect_run!!.get_results_bitmap(bitmap)
        var action_info: ActionInfo? = null

        if (res.isNotEmpty()) {
            var target = res[0]
            if (target.click) {
                var action = target.lb.getString("action")
                val notify = target.lb.optJSONObject("notify")

                var xy = utils.click_xy(target.getLocation())

                if (action == "no_action") {
                    action_info = null
                } else if (xy != null && action == "click") {
                    action_info = ActionInfo(xy.get(0), xy.get(1), action)
                } else if (xy != null && action == "swipe") {
                    var t_rect = target.getLocation()
                    t_rect.left
                    action_info = ActionInfo(xy.get(0), xy.get(1), action)
                }

                //박스보이게
                if (bsView.rect_view_visible()) {
                    Handler(Looper.getMainLooper()).post {
                        bsView.draw_rect_show(res)
                    }
                }

                //분해카운터
                Handler(Looper.getMainLooper()).post {
                    bsView.update_tv_disassembly_counter()
                }


                //알림발송
                if (kakao_send_notify && notify != null && notify.getBoolean("use")) {
                    notify_kakao(notify.getString("txt"))
                }
            }

        }

        return action_info
    }

    inner class BackgroundThread : Thread() {

        fun exit() {
            sleep(1000)
            interrupt()
            Log.d("종료", "=====================쓰레드.exit======================")
        }

        override fun run() {
            while (true && !isInterrupted) {
                if (BS_THREAD && !RUN_DETECT && !isInterrupted && detect_run?.detector?.run_state == true) {


                    val startTime = SystemClock.uptimeMillis()
                    val bitmap = image_available_bitmap()
                    lastProcessingTimeMs_capture = SystemClock.uptimeMillis() - startTime



                    if (bitmap != null && detect_run != null && !isInterrupted) {
                        val startTime = SystemClock.uptimeMillis()
                        RUN_DETECT = true
                        var act_info: ActionInfo? = tflite_bitmap(bitmap)
                        RUN_DETECT = false
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                        Log.d("시간", "bitmap to jpg: " + lastProcessingTimeMs_capture + "ms")
                        Log.d("시간", "tflite_run:  : " + lastProcessingTimeMs + "ms")
                        if (act_info != null) {
                            val x = act_info.x
                            val y = act_info.y

                            if (!bsView.tv_RectF.contains(x, y)) {
                                if (user_calickable) {
                                    touchService.click(act_info)
                                }
                                Handler(Looper.getMainLooper()).post {
                                    bsView.draw_effect(x, y)
                                }
                            } else {
                                Log.d("탑뷰-좌표", "광고영역에 들어왔습니다.")
                            }

                        } else {

                        }
                    } else {
                        RUN_DETECT = false
                    }
                }


            }


        }

    }

    override fun onDestroy() {
        BS_THREAD = false
        bsThread.exit()
        close()
        bsView.close()


        //touchService.disableSelf()

        Toast.makeText(
            this,
            applicationContext.getString(R.string.app_service_stop),
            Toast.LENGTH_SHORT
        ).show()



        Log.d("종료", "=====================끝=====================")
    }


    fun run_notify() {
        var noti = Noti(this)
        noti.createNotificationChannel()
        var notify = noti.build(11232131)
        startForeground(FOREGROUND_SERVICE_ID, notify)

    }

    fun ready_media() {
        STORE_DIRECTORY = utils.mkdir()
    }


}


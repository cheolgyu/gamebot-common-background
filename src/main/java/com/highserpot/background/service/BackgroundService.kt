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
import com.highserpot.background.clickable
import com.highserpot.background.notification.Noti
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
        Log.d("클릭 notify_kakao", string.toString())
        val title = string
        val defaultText = TextTemplate(
            text = title + """ """.trimIndent(),
            link = Link(
                webUrl = "https://play.google.com/store/apps/details?id=com.highserpot.rok",
                mobileWebUrl = "https://play.google.com/store/apps/details?id=com.highserpot.rok"
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

    fun tflite_bitmap(bitmap: Bitmap): FloatArray? {

        val res = detect_run!!.get_results_bitmap(bitmap)


        var c_xy: FloatArray? = null
        if (res.isNotEmpty()) {
            c_xy = utils.click_xy(res[0].getLocation())
        }
        Log.d("클릭 객체", res.toString())

        if (res.isNotEmpty() && bsView.rect_view_visible()) {
            Handler(Looper.getMainLooper()).post(Runnable {
                bsView.draw_rect_show(res)
            })
        }

        if (res.isNotEmpty()) {
            //루프중 이번 인식목록의 글릭가능 가능여부는 인식목록 안에 있는 객체의 click에 저장함.
            Log.d("클릭 객체", (!res[0].click).toString())
            if (!res[0].click) {
                val notify = res[0].lb.optJSONObject("notify")
                Log.d("클릭 kakao_send_notify", kakao_send_notify.toString())
                if (kakao_send_notify && notify != null && notify.getBoolean("use")) {
                    notify_kakao(notify.getString("txt"))
                }

            }

            // lb.click_object: 인식객체가 클릭객체 인지 아닌지의 구분
            if (!res[0].lb.getBoolean("click_object")) {


                return null
            }
        }

        return c_xy
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
                    val lastProcessingTimeMs_bitmap = SystemClock.uptimeMillis() - startTime



                    if (bitmap != null && detect_run != null && !isInterrupted) {
                        val startTime = SystemClock.uptimeMillis()
                        RUN_DETECT = true
                        var arr: FloatArray? = tflite_bitmap(bitmap)
                        RUN_DETECT = false
                        var lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                        Log.d("시간", "bitmap to jpg: " + lastProcessingTimeMs_bitmap + "ms")
                        Log.d("시간", "tflite_run:  : " + lastProcessingTimeMs + "ms")

                        if (arr != null) {
                            val x = arr.get(0)
                            val y = arr.get(1)

                            if (!bsView.tv_RectF.contains(x, y)) {
                                if (clickable){
                                    touchService.click(x, y)
                                }
                                Handler(Looper.getMainLooper()).post(Runnable {
                                    bsView.draw_effect(x, y)
                                })
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


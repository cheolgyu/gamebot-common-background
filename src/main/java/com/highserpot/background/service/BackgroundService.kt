package com.highserpot.background.service


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.*
import com.highserpot.background.R
import com.highserpot.background.Utils
import com.highserpot.background.notification.Noti
import java.io.FileOutputStream
import java.nio.ByteBuffer


class BackgroundService : BackgroundServiceMP() {

    var STORE_DIRECTORY: String? = null
    private val FOREGROUND_SERVICE_ID = 1000

    lateinit var utils: Utils
    lateinit var bsView: BackgroundServiceView
    lateinit var bsThread: BackgroundThread

    override fun onCreate() {
        utils = Utils(this.applicationContext)

        BS_THREAD = true
        bsThread = BackgroundThread()
        bsThread!!.start()

        run_notify()
        ready_media()
        bsView = BackgroundServiceView(applicationContext)


    }

    @Throws(java.lang.Exception::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {

            my_resultCode = intent.getIntExtra("resultCode", 1000)
            my_data = intent.getParcelableExtra("data")

            createVirtualDisplay()
            createModel()
            bsView.start()

        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    @Throws(Exception::class)
    fun image_available(): String? {
        var image = imageReader?.acquireNextImage()
        if (image != null) {
            var fos: FileOutputStream? = null
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride: Int = planes[0].pixelStride
            val rowStride: Int = planes[0].rowStride
            val rowPadding: Int = rowStride - pixelStride * mWidth

            var w: Int = mWidth + rowPadding / pixelStride

            var bitmap: Bitmap? = null
            try {
                bitmap = Bitmap.createBitmap(
                    w,//+ rowPadding / pixelStride,
                    mHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
            } catch (e: Exception) {
                Log.e(
                    "---",
                    e.printStackTrace().toString()
                )
            } finally {
                image.close()
            }


            // write bitmap to a file

            // write bitmap to a file
            val file_id = System.currentTimeMillis()
            var my_file = STORE_DIRECTORY + file_id + ".jpg"
            fos =
                FileOutputStream(my_file)
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()

            return my_file

        }

        return null
    }

    fun tflite_run(full_path: String): FloatArray? {

        detect_run.build(mWidth, mHeight)
        var res = detect_run.get_results(full_path)


        var c_xy: FloatArray? = null
        if (res.isNotEmpty()) {
            c_xy = utils.click_xy(res[0].title.toInt(), res[0].getLocation())
        }

        when (applicationContext.packageName) {
            "com.highserpot.baram" -> {
                if (res.isNotEmpty()) {
                    c_xy = if (res[0].title.toInt() == 1) {
                        null
                    } else {
                        utils.click_xy(res[0].title.toInt(), res[0].getLocation())
                    }
                }
            }
            "com.highserpot.v4" -> {
                if (res.isNotEmpty()) {
                    c_xy = if (res[0].title.toInt() == 1) {
                        null
                    } else if (res[0].title.toInt() == 4) {
                        var arr = FloatArray(2)

                        val item = res[0].getLocation()
                        val x = item.left + (item.right - item.left) / 2
                        val y = item.bottom
                        arr.set(0, x)
                        arr.set(1, y)
                        arr
                    } else {
                        utils.click_xy(res[0].title.toInt(), res[0].getLocation())
                    }
                }
            }
            "com.highserpot.gotgl" -> {
                if (res.isNotEmpty()) {
                    c_xy = if (res[0].title.toInt() == 4) {
                        res.removeAll { recognition -> recognition.title.toInt() == 4 }
                        if (res.size > 0) {
                            utils.click_xy(res[0].title.toInt(), res[0].getLocation())
                        } else {
                            null
                        }
                    } else {
                        utils.click_xy(res[0].title.toInt(), res[0].getLocation())
                    }
                } else {
                    Log.d("예측결과", "빈값왔다." + mWidth.toString())
                    val px: Float = (mWidth - 10).toFloat()
                    val py: Float = ((mHeight / 2)).toFloat()
                    val p_lb = RectF((mWidth / 2).toFloat(), (mHeight / 2).toFloat(), 1F, 1F)
                    val label = getString(R.string.wakeup)
                    c_xy = FloatArray(2).apply {
                        set(0, px)
                        set(1, py)
                    }

                    Handler(Looper.getMainLooper()).post(Runnable {
                        bsView.draw_rect(p_lb, label)
                    })
                }
            }
            "com.highserpot.illusionc" -> {
                if (res.isNotEmpty()) {
                    c_xy = if (res[0].title.toInt() == 6) {
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            bsView.stop()
                        }, 1)

                        null
                    } else {
                        utils.click_xy(res[0].title.toInt(), res[0].getLocation())
                    }
                }
            }
            else -> {
                if (res.isNotEmpty()) {
                    c_xy = utils.click_xy(res[0].title.toInt(), res[0].getLocation())
                }
            }
        }

        if (res != null && res.size >= 1 && bsView.rect_view_visible()) {
            Handler(Looper.getMainLooper()).post(Runnable {
                bsView.draw_rect_show(res)
            })
        }

        Log.d("예측정리", res.toString())

        //if (!BuildConfig.DEBUG) {
        utils.rm_full_path(full_path)
        //}
        return c_xy
    }

    inner class BackgroundThread : Thread() {

        override fun run() {
            while (true) {
                if (BS_THREAD && !RUN_DETECT) {
                    //화면 갱신하게 시간줌. 대화 다나올 시간
                    //Thread.sleep(1000)
                    //image_available 기다리는시간.
                    Thread.sleep(300)

                    var full_path = image_available()

                    if (full_path != null && full_path != "") {
                        val startTime = SystemClock.uptimeMillis()
                        RUN_DETECT = true
                        var arr: FloatArray? = tflite_run(full_path)
                        RUN_DETECT = false
                        var lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                        Log.d("예측-시간", "Inference time: " + lastProcessingTimeMs + "ms")

                        if (arr != null) {
                            val x = arr.get(0)
                            val y = arr.get(1)

                            // if (!BuildConfig.DEBUG) {
                            if (!bsView.tv_RectF.contains(x, y)) {
                                touchService.click(x, y)
                            }
                            // }

                            Handler(Looper.getMainLooper()).post(Runnable {
                                bsView.draw_effect(x, y)
                            })

                            //터치후 화면 갱신하게 시간줌.
                            //Thread.sleep(300)
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
        bsView.stop()
        bsView.destroy()
        orientationChangeCallback.disable()
        virtualDisplay = null
        mediaProjection?.stop()
        Toast.makeText(
            this,
            applicationContext.getString(R.string.app_service_stop),
            Toast.LENGTH_SHORT
        ).show()
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


package com.highserpot.background.service

//import com.example.tf.tflite.Run
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import android.widget.Toast
import com.highserpot.background.R
import com.highserpot.background.notification.Noti
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


class BackgroundService : BackgroundServiceMP() {

    var STORE_DIRECTORY: String? = null
    var mBackgroundThread: BackgroundThread? = null
    private val FOREGROUND_SERVICE_ID = 1000
    val TAG: String = "BackgroundService"
    var my_action: String? = null

    override fun onCreate() {
        run_notify()
        ready_media()
    }

    @Throws(java.lang.Exception::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        RUN_BACKGROUND = true
        my_resultCode = intent!!.getIntExtra("resultCode", 1000)
        my_data = intent.getParcelableExtra("data")

        createVirtualDisplay()

        // start capture handling thread
        mBackgroundThread = BackgroundThread()
        mBackgroundThread!!.start()

        Toast.makeText(
            this,
            applicationContext.getString(R.string.app_service_start),
            Toast.LENGTH_SHORT
        ).show()

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    //mp 서비스에서 구현
    @Throws(Exception::class)
    fun image_available(): String? {
        Thread.sleep(1000)
        var image = imageReader!!.acquireLatestImage()
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
            var my_file = STORE_DIRECTORY + file_id + ".JPEG"
            fos =
                FileOutputStream(my_file)
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()

            return my_file

        }

        return null
    }


    fun tflite_run(full_path: String): FloatArray? {
        val so = getScreenOrientation()
        var run = com.highserpot.tf.tflite.Run(this, so)
        run.build(full_path)
        var res = run.get_xy(full_path)

        //return null
        return res
    }


    fun run_notify() {
        var noti = Noti(this)
        noti.createNotificationChannel()
        var notify = noti.build(11232131)
        startForeground(FOREGROUND_SERVICE_ID, notify)

    }

    fun ready_media() {
        mkdir()
    }

    fun mkdir() {
        val externalFilesDir = getExternalFilesDir(null)
        if (externalFilesDir != null) {
            STORE_DIRECTORY =
                externalFilesDir.absolutePath + "/screenshots/"
            val storeDirectory =
                File(STORE_DIRECTORY)
            storeDirectory.deleteRecursively()
            if (!storeDirectory.exists()) {

                val success: Boolean = storeDirectory.mkdirs()
                if (!success) {
                    Log.e(
                        TAG,
                        "failed to create file storage directory."
                    )
                    return
                }
            }
        } else {
            Log.e(
                TAG,
                "failed to create file storage directory, getExternalFilesDir is null."
            )
            return
        }
    }

    inner class BackgroundThread : Thread() {

        override fun run() {
            while (RUN_BACKGROUND) {
                Thread.sleep(1000)

                var full_path = image_available()

                if (full_path != null && full_path != "") {
                    var arr: FloatArray? = tflite_run(full_path)
                    if (arr != null) {
                        var x = arr.get(0)
                        var y = arr.get(1)

                        touchService!!.click(x, y)
                    } else {

                    }
                } else {

                }
            }
        }

    }


}
package com.highserpot.background.service


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.Image
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.view.marginTop
import com.highserpot.background.R
import com.highserpot.background.notification.Noti
import com.highserpot.yolov4.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


class BackgroundService : BackgroundServiceMP() {

    var STORE_DIRECTORY: String? = null
    var mBackgroundThread: BackgroundThread? = null
    private val FOREGROUND_SERVICE_ID = 1000
    val TAG: String = "BackgroundService"
    lateinit var onTopView: View
    lateinit var effectView: View
    lateinit var manager: WindowManager
    var prevX = 0f
    var prevY = 0f
    lateinit var window_params: WindowManager.LayoutParams
    lateinit var window_params_effect: WindowManager.LayoutParams
    lateinit var imageView: ImageView

    lateinit var btn_switch: Switch

    override fun onCreate() {
        run_notify()
        ready_media()
        top_view()
        effect_view()
    }

    fun draw_effect(x: Float, y: Float) {
        val view = (imageView as View)
        view.x = x - (imageView.width / 2)
        view.y = y - (imageView.height / 2)
    }

    fun set_effect() {
        imageView = ImageView(effectView.context)
        imageView.setImageResource(R.mipmap.ic_launcher_round)

        (effectView as LinearLayout).addView(imageView)
        effectView.setBackgroundColor(Color.TRANSPARENT)
    }

    fun effect_view() {

        val LAYOUT_FLAG: Int
        LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val inflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        effectView = inflater.inflate(R.layout.effect_layout, null)
        set_effect()

        window_params_effect = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        window_params_effect.gravity = Gravity.LEFT or Gravity.TOP

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(effectView, window_params_effect)

    }

    @SuppressLint("ClickableViewAccessibility")
    @Throws(java.lang.Exception::class)
    fun top_view() {
        val LAYOUT_FLAG: Int
        LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val inflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        onTopView = inflater.inflate(R.layout.always_on_top_layout, null)
        // onTopView!!.setOnTouchListener(this)

        window_params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        window_params.gravity = Gravity.LEFT or Gravity.TOP

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(onTopView, window_params)

        //val btn_move: Button = onTopView.findViewById(R.id.btn_move)
        onTopView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(arg0: View?, arg1: MotionEvent): Boolean {

                return move(arg0!!, arg1)
            }
        })
        btn_switch = onTopView.findViewById(R.id.btn_switch)
        btn_switch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(
                buttonView: CompoundButton,
                isChecked: Boolean
            ) {
                if (isChecked) {
                    buttonView.setTextColor(Color.BLUE)
                    buttonView.text = applicationContext.getString(R.string.over_start_txt)
                    start_thread()
                    (imageView as View).visibility = View.VISIBLE
                } else {
                    buttonView.setTextColor(Color.BLACK)
                    buttonView.text = applicationContext.getString(R.string.over_stop_txt)
                    stop_thread()
                    (imageView as View).visibility  = View.INVISIBLE
                }
            }
        })
    }

    fun start() {
        btn_switch.isChecked = true
    }

    fun stop() {
        btn_switch.isChecked = false
    }

    fun start_thread() {
        RUN_BACKGROUND = true
        // start capture handling thread
        mBackgroundThread = BackgroundThread()
        mBackgroundThread!!.start()

        Toast.makeText(
            this,
            applicationContext.getString(R.string.app_service_thread_start),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun stop_thread() {
        RUN_BACKGROUND = false
        Toast.makeText(
            this,
            applicationContext.getString(R.string.app_service_thread_stop),
            Toast.LENGTH_SHORT
        ).show()
    }


    @Throws(java.lang.Exception::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {

            my_resultCode = intent.getIntExtra("resultCode", 1000)
            my_data = intent.getParcelableExtra("data")

            createVirtualDisplay()
            createModel()
            start()

        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    @Throws(Exception::class)
    fun image_available(): String? {
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

        detect_run.build(mWidth, mHeight)
        var res = detect_run.get_xy(full_path)
        if (!BuildConfig.DEBUG) {
            rm_full_path(full_path)
        }
        return res
    }

    fun rm_full_path(full_path: String) {
        var f = File(full_path)
        if (f.exists()) {
            f.delete()
        }
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
                //화면 갱신하게 시간줌. 대화 다나올 시간
                //Thread.sleep(1500)
                var full_path = image_available()

                if (full_path != null && full_path != "") {

                    var arr: FloatArray? = tflite_run(full_path)


                    if (arr != null) {
                        val x = arr.get(0)
                        val y = arr.get(1)

                        touchService.click(x, y)
                        Handler(Looper.getMainLooper()).post(Runnable {
                            draw_effect(x, y)
                        })

                        //터치후 화면 갱신하게 시간줌.
                        //Thread.sleep(300)
                    } else {

                    }
                } else {

                }
            }
        }

    }

    fun move(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.rawX
                prevY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val rawX: Float = event.rawX // 절대 X 좌표 값을 가져온다.
                val rawY: Float = event.rawY // 절대 Y 좌표값을 가져온다.

                // 이동한 위치에서 처음 위치를 빼서 이동한 거리를 구한다.
                val x: Float = rawX - prevX
                val y: Float = rawY - prevY
                setCoordinateUpdate(x, y)
                prevX = rawX
                prevY = rawY
            }
        }
        return false
    }

    private fun setCoordinateUpdate(x: Float, y: Float) {
        if (window_params != null) {
            window_params.x += x.toInt()
            window_params.y += y.toInt()
            manager.updateViewLayout(onTopView, window_params)
        }
    }


    override fun onDestroy() {
        stop()
        manager.removeView(onTopView)
        manager.removeView(effectView)
        orientationChangeCallback.disable()
        virtualDisplay = null
        mediaProjection?.stop()
        Toast.makeText(
            this,
            applicationContext.getString(R.string.app_service_stop),
            Toast.LENGTH_SHORT
        ).show()
    }
}


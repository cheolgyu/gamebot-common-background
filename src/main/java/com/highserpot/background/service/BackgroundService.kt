package com.highserpot.background.service


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.Image
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.*
import com.highserpot.background.BuildConfig
import com.highserpot.background.R
import com.highserpot.background.Utils
import com.highserpot.background.effect.PointLayout
import com.highserpot.background.effect.RectLayout
import com.highserpot.background.notification.Noti
import java.io.FileOutputStream
import java.nio.ByteBuffer


class BackgroundService : BackgroundServiceMP() {

    var STORE_DIRECTORY: String? = null
    var mBackgroundThread: BackgroundThread? = null
    private val FOREGROUND_SERVICE_ID = 1000
    lateinit var onTopView: View
    lateinit var effectView: View
    lateinit var rectView: View

    lateinit var manager: WindowManager
    var prevX = 0f
    var prevY = 0f
    lateinit var window_params: WindowManager.LayoutParams
    lateinit var window_params_effect: WindowManager.LayoutParams

    lateinit var btn_switch: Switch
    lateinit var rect_switch: Switch
    lateinit var utils: Utils

    override fun onCreate() {
        utils = Utils(this.applicationContext)
        run_notify()
        ready_media()
        add_view_top()
        add_view_effect()
        add_view_rect()
    }

    fun add_view_rect() {

        rectView = RectLayout(applicationContext)
        window_params_effect = utils.get_wm_lp(false)

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(rectView, window_params_effect)

    }

    fun add_view_effect() {

        effectView = PointLayout(applicationContext)
        window_params_effect = utils.get_wm_lp(false)
        window_params_effect.gravity = Gravity.LEFT or Gravity.CENTER

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(effectView, window_params_effect)

    }

    @SuppressLint("ClickableViewAccessibility")
    @Throws(java.lang.Exception::class)
    fun add_view_top() {

        val inflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        onTopView = inflater.inflate(R.layout.always_on_top_layout, null)
        // onTopView!!.setOnTouchListener(this)

        window_params = utils.get_wm_lp(true)
        window_params.flags
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
                    effectView.visibility = View.VISIBLE
                } else {
                    buttonView.setTextColor(Color.BLACK)
                    buttonView.text = applicationContext.getString(R.string.over_stop_txt)
                    stop_thread()
                    effectView.visibility = View.INVISIBLE
                }
            }
        })

        rect_switch = onTopView.findViewById(R.id.rect_switch)
        rect_switch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(
                buttonView: CompoundButton,
                isChecked: Boolean
            ) {
                if (isChecked) {
                    buttonView.setTextColor(Color.BLUE)
                    buttonView.text = applicationContext.getString(R.string.rect_layout_start_txt)
                    rectView.visibility = View.VISIBLE
                } else {
                    buttonView.setTextColor(Color.BLACK)
                    buttonView.text = applicationContext.getString(R.string.rect_layout_stop_txt)
                    rectView.visibility = View.INVISIBLE
                }
            }
        })
    }

    fun start() {
        btn_switch.isChecked = true
        rect_switch.isChecked = true
    }

    fun stop() {
        btn_switch.isChecked = false
        rect_switch.isChecked = false
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

        if (res != null && res.size >= 1 && rectView != null && rectView.visibility == View.VISIBLE) {
            Handler(Looper.getMainLooper()).post(Runnable {
                (rectView as RectLayout).show(res)
            })
        }

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
            else -> {
                if (res.isNotEmpty()) {
                    c_xy = utils.click_xy(res[0].title.toInt(), res[0].getLocation())
                }
            }
        }

        Log.d("tflite_run", res.toString())

        if (!BuildConfig.DEBUG) {
            utils.rm_full_path(full_path)
        }
        return c_xy
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


    inner class BackgroundThread : Thread() {

        override fun run() {
            while (RUN_BACKGROUND) {
                //화면 갱신하게 시간줌. 대화 다나올 시간
                //Thread.sleep(1000)

                var full_path = image_available()

                if (full_path != null && full_path != "") {
                    val startTime = SystemClock.uptimeMillis()
                    var arr: FloatArray? = tflite_run(full_path)
                    var lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                    Log.d("예측-시간", "Inference time: " + lastProcessingTimeMs + "ms")

                    if (arr != null) {
                        val x = arr.get(0)
                        val y = arr.get(1)

                        if (!BuildConfig.DEBUG) {
                            touchService.click(x, y)
                        }

                        Handler(Looper.getMainLooper()).post(Runnable {
                            (effectView as PointLayout).draw(x, y)
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


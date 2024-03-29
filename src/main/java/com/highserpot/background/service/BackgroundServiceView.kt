package com.highserpot.background.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.*
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.ads.*
import com.highserpot.background.*
import com.highserpot.background.R
import com.highserpot.background.effect.PointLayout
import com.highserpot.background.effect.RectLayout
import com.highserpot.background.service.BackgroundServiceMP.Companion.disassembly_counter
import com.highserpot.tf.tflite.Classifier

class BackgroundServiceView(var ctx: Context) {

    lateinit var onTopView: View
    lateinit var effectView: View
    lateinit var rectView: View
    lateinit var window_params: WindowManager.LayoutParams
    lateinit var window_params_effect: WindowManager.LayoutParams
    lateinit var btn_switch: Switch
    lateinit var btn_on_off: ImageView
    lateinit var area_on_off: LinearLayout
    lateinit var rect_switch: Switch
    lateinit var tv_disassembly_counter: TextView
    lateinit var utils: Utils
    lateinit var manager: WindowManager

    var prevX = 0f
    var prevY = 0f
    var tv_RectF: RectF = RectF()

    init {
        utils = Utils(ctx)
        load_view()
        load_view_effect()
        load_view_rect()
        load_admob_start()
    }

    fun update_tv_disassembly_counter() {
        tv_disassembly_counter.text = disassembly_counter.toString()
    }

    fun draw_effect(x: Float, y: Float) {
        (effectView as PointLayout).draw(x, y)
    }

    fun draw_rect(p_lb: RectF, label: String) {
        (rectView as RectLayout).show_lable(p_lb, label)
    }

    fun draw_rect_show(res: MutableList<Classifier.Recognition>) {
        (rectView as RectLayout).show(res)
    }

    fun rect_view_visible(): Boolean {
        return rectView != null && rectView.visibility == View.VISIBLE
    }

    fun start() {
        btn_switch.isChecked = true
        rect_switch.isChecked = true
    }

    fun stop() {
        btn_switch.isChecked = false
        rect_switch.isChecked = false
    }

    fun close() {
        stop()
        manager.removeView(onTopView)
        manager.removeView(effectView)
    }

    fun load_view_rect() {

        rectView = RectLayout(ctx)
        window_params_effect = utils.get_wm_lp(false)

        manager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(rectView, window_params_effect)

    }

    fun load_view_effect() {

        effectView = PointLayout(ctx)
        window_params_effect = utils.get_wm_lp(false)
        window_params_effect.gravity = Gravity.LEFT or Gravity.TOP

        manager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(effectView, window_params_effect)

    }

    fun load_view() {

        val inflater =
            ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        onTopView = inflater.inflate(R.layout.always_on_top_layout, null)

        window_params = utils.get_wm_lp(true)
        window_params.flags
        window_params.gravity = Gravity.LEFT or Gravity.CENTER

        manager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(onTopView, window_params)


        listener_load_view()

        onTopView.getViewTreeObserver().addOnGlobalLayoutListener(OnGlobalLayoutListener {

            tv_update()
        })

    }

    @SuppressLint("ClickableViewAccessibility")
    fun listener_load_view() {

        tv_disassembly_counter = onTopView.findViewById<TextView>(R.id.tv_disassembly_counter)
        var tv_disassembly_counter_init =
            onTopView.findViewById<Button>(R.id.tv_disassembly_counter_init)
        tv_disassembly_counter_init.setOnClickListener {
            disassembly_counter = 0
            update_tv_disassembly_counter()
        }

        btn_on_off = onTopView.findViewById(R.id.btn_on_off)
        area_on_off = onTopView.findViewById(R.id.area_on_off) as LinearLayout


        onTopView.findViewById<Switch>(R.id.clickable_switch)
            .setOnCheckedChangeListener { buttonView, isChecked ->
                CheckTouch(ctx).checkAccessibilityPermissions()

                user_calickable = isChecked && device_clickable
                buttonView.isChecked = user_calickable
            }


        area_on_off = onTopView.findViewById(R.id.area_on_off) as LinearLayout
        btn_on_off.setOnTouchListener { arg0, arg1 -> move(arg0!!, arg1) }
        btn_on_off.setOnClickListener {
            if (area_on_off.visibility == View.VISIBLE) {
                area_on_off.visibility = View.GONE
                DrawableCompat.setTint(
                    btn_on_off.drawable,
                    ctx.getColor(R.color.ic_launcher_background)
                )
            } else {
                area_on_off.visibility = View.VISIBLE
                DrawableCompat.setTint(
                    btn_on_off.drawable,
                    ctx.getColor(R.color.browser_actions_title_color)
                )
            }
        }

        btn_switch = onTopView.findViewById(R.id.btn_switch)
        btn_switch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(
                buttonView: CompoundButton,
                isChecked: Boolean
            ) {
                if (isChecked) {
                    buttonView.setTextColor(Color.BLUE)
                    //buttonView.text = applicationContext.getString(R.string.over_start_txt)
                    BS_THREAD = true
                    effectView.visibility = View.VISIBLE
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.app_service_thread_start),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    buttonView.setTextColor(Color.BLACK)
                    //buttonView.text = applicationContext.getString(R.string.over_stop_txt)
                    BS_THREAD = false
                    effectView.visibility = View.INVISIBLE
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.app_service_thread_stop),
                        Toast.LENGTH_SHORT
                    ).show()
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
                    rectView.visibility = View.VISIBLE
                } else {
                    buttonView.setTextColor(Color.BLACK)
                    rectView.visibility = View.INVISIBLE
                }
            }
        })
    }

    fun load_admob_start() {
        Log.d("탑뷰-광고", "load_admob")
        val mAdView: AdView = onTopView.findViewById(R.id.always_on_top_adview)
        val adRequest = AdRequest.Builder().build()
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("탑뷰-광고", "onAdLoaded")
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                Log.d("탑뷰-광고", "onAdFailedToLoad:" + errorCode.toString())
            }

            override fun onAdOpened() {
                Log.d("탑뷰-광고", "onAdOpened")
            }

            override fun onAdClicked() {
                Log.d("탑뷰-광고", "onAdClicked")
            }

            override fun onAdLeftApplication() {
                Log.d("탑뷰-광고", "onAdLeftApplication")
            }

            override fun onAdClosed() {
                Log.d("탑뷰-광고", "onAdClosed")
                // Code to be executed when the interstitial ad is closed.
                mAdView.loadAd(AdRequest.Builder().build())
            }
        }
        mAdView.loadAd(adRequest)
    }

    fun move(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.getRawX()
                prevY = event.getRawY()
            }

            MotionEvent.ACTION_MOVE
            -> {
                val rawX: Float = event.getRawX()
                val rawY: Float = event.getRawY()

                val x: Float = rawX - prevX
                val y: Float = rawY - prevY
                setCoordinateUpdate(x, y)
                prevX = rawX
                prevY = rawY


                tv_update()
            }
        }
        return false
    }

    fun tv_update() {
        val point = IntArray(2)
        onTopView.getLocationOnScreen(point) // or getLocationInWindow(point)
        onTopView.width
        val (x, y) = point
        tv_RectF = RectF(
            x.toFloat(),
            y.toFloat(),
            x.toFloat() + onTopView.width,
            y.toFloat() + onTopView.height
        )
    }

    private fun setCoordinateUpdate(x: Float, y: Float) {
        if (window_params != null) {
            window_params.x += x.toInt()
            window_params.y += y.toInt()

            manager.updateViewLayout(onTopView, window_params)
        }
    }

}
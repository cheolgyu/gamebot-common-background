package com.highserpot.background.effect

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatDelegate
import com.highserpot.background.R

class PointLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        this.setBackgroundColor(Color.TRANSPARENT)
    }

    var sec = 500

    fun get_image_view(cx: Float, cy: Float): ImageView {
        return ImageView(this.context).apply {
            setImageResource(R.drawable.ic_baseline_pets_100)
            setBackgroundColor(Color.TRANSPARENT)
            x = cx- drawable.intrinsicWidth / 2
            y = cy-drawable.intrinsicHeight / 2
        }
    }

    fun draw(cx: Float, cy: Float) {
        val iv = get_image_view(cx, cy)

        this.addView(iv)
        val mMyTask = Runnable {
            (this as RelativeLayout).removeView(iv)
        }
        val mHandler = Handler()
        mHandler.postDelayed(mMyTask, sec.toLong())
    }


}
package com.highserpot.background.effect

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Handler
import android.util.AttributeSet
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import com.highserpot.background.R
import com.highserpot.tf.tflite.Classifier


class RectLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
     var color : Int

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        this.setBackgroundColor(Color.TRANSPARENT)
        color = context.applicationContext.getColor(R.color.colorPrimary)
    }

    val sec = 300

    fun get_image_view(location: RectF): ImageView {
        val sd = ShapeDrawable().apply {
            intrinsicWidth = ((location.right - location.left)).toInt()
            intrinsicHeight = ((location.bottom - location.top)).toInt()
            shape = RectShape()
            paint.color = color
            paint.alpha = 255

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f
            paint.strokeJoin = Paint.Join.ROUND
        }

        var iv = ImageView(this.context).apply {
            setImageDrawable(sd)
            x = location.left
            y = location.top
        }

        return iv
    }

    fun get_image_view_label(location: RectF, txt:String): TextView {

        var label = TextView(this.context).apply {
            x = location.left
            y = location.top-70f
            text = txt
            textSize = 20f
            setTextColor(color)
        }

        return label
    }

    fun make_item(location: RectF, title: String) {
        var iv = get_image_view(location)
        var tv = get_image_view_label(location,title)

        this.addView(iv)
        this.addView(tv)

        val mMyTask = Runnable {
            this.removeView(iv)
            this.removeView(tv)
        }
        val mHandler = Handler()
        mHandler.postDelayed(mMyTask, sec.toLong())

    }

    fun show(list: List<Classifier.Recognition>) {
        for (item in list) {
            make_item(item.getLocation(),item.title)
        }
    }


}
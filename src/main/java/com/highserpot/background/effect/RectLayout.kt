package com.highserpot.background.effect

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Handler
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.highserpot.background.R
import com.highserpot.tf.tflite.Classifier


class RectLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    var color: Int

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        this.setBackgroundColor(Color.TRANSPARENT)
        color = context.applicationContext.getColor(R.color.box)
    }

    var sec = 300

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

    fun get_image_view_label(location: RectF, txt: String): TextView {

        var label = TextView(this.context).apply {
            textSize = 50f
            x = location.left
            y = location.top - (textSize + 20f)
            text = txt + " "
            setBackgroundColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD_ITALIC)
            setTextColor(color)
        }

        return label
    }

    fun make_item(location: RectF, title: String) {
        var iv = get_image_view(location)
        var tv = get_image_view_label(location, title)

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
            make_item(item.getLocation(), item.lb.getString("name"))
        }
    }

    fun show_lable(item: RectF, lable: String) {
        make_item(item, lable)
    }


}
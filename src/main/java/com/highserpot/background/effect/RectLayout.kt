package com.highserpot.background.effect

import android.R.attr.fillColor
import android.R.attr.strokeWidth
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import com.highserpot.background.R
import com.highserpot.tf.tflite.Classifier


class RectLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        this.setBackgroundColor(Color.TRANSPARENT)
    }
    val sec = 300

    fun make_item(location: RectF) {
        val gd = GradientDrawable()
        gd.shape = GradientDrawable.RECTANGLE
        gd.setStroke(10, Color.RED)
        gd.setSize(((location.right-location.left)).toInt(),((location.bottom-location.top)).toInt())




        var iv = ImageView(this.context)

        //iv.setImageResource(R.drawable.rect)
        iv.setImageDrawable(gd)
        (this as LinearLayout).addView(iv)
        var view = iv  as View

//        val params =
//            iv.getLayoutParams()
//        params.width = (location.left-location.right).toInt()
//        params.height = (location.bottom-location.top).toInt()
//        iv.setLayoutParams(params)


        view.x = location.left - (location.right-location.left)/2
        view.y = location.top-(location.bottom-location.top)/2
        val mMyTask = Runnable {
            (this as LinearLayout).removeView(view)
        }
        val mHandler = Handler()
        mHandler.postDelayed(mMyTask, sec.toLong())

    }

    fun show(list : List<Classifier.Recognition>){
        for (item in list){
            make_item(item.getLocation())
        }

    }


}
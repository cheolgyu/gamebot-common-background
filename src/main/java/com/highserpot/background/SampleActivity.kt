package com.highserpot.background

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.InputStream


class SampleActivity : AppCompatActivity() {
    private val TAG: String? = SampleActivity::class.simpleName
    private val DIR: String = "sample"

    var mWidth = 0
    var mHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_sample)
        Log.d(TAG, TAG.toString())


        load_wh()
        load_sample()
    }

    fun load_wh() {
        var metrics = DisplayMetrics()
        var display = windowManager.defaultDisplay
        windowManager.defaultDisplay.getMetrics(metrics)
        display!!.getMetrics(metrics)
        var size = Point()
        display.getRealSize(size)
        mWidth = size.x
        mHeight = size.y
    }

    fun getBitmapFromAssets(fileName: String): Bitmap? {
        val assetManager = assets
        val istr: InputStream = assetManager.open(DIR + File.separator + fileName)
        val bitmap = BitmapFactory.decodeStream(istr)
        istr.close()

        return Bitmap.createScaledBitmap(bitmap, mWidth, mHeight, true)
    }

    fun load_sample() {


        var li = assets.list(DIR)


        if (li != null) {
            var ll = findViewById(R.id.ll) as LinearLayout

            for (item in li) {
                Log.d(TAG, "${item}")
                var bitmap = getBitmapFromAssets(item)

                var iv = ImageView(this)
                iv.setImageBitmap(bitmap)
                ll.addView(iv)
            }
        } else {
        }
    }
}
package com.highserpot.tf.tflite

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import com.highserpot.background.BuildConfig

import com.highserpot.tf.env.ImageUtils
import com.highserpot.tf.tflite.Classifier.Recognition
import com.highserpot.tf.tracking.MultiBoxTracker


class Run(val context: Context) {
    private var tracker: MultiBoxTracker? = null
    private var ori_tracker: MultiBoxTracker? = null
    protected var previewWidth = 0
    protected var previewHeight = 0
    private var sensorOrientation: Int? = null
    private var timestamp: Long = 0

    private val IS_MODEL_QUANTIZED = false
    private val MODEL_FILE = "detect.tflite"

    var detector: Classifier? = null
    private var croppedBitmap: Bitmap? = null
    private var oriBitmap: Bitmap? = null
    private var frameToCropTransform: Matrix? = null
    private var ori_frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null
    var frameToCanvasMatrix: Matrix? = null

    lateinit var assetManager: AssetManager

    init {
        Log.d("run", "==========================init====================")
        assetManager =
            context.applicationContext.assets
        detector = YoloV4Classifier.create(
            assetManager,
            MODEL_FILE,
            IS_MODEL_QUANTIZED
        )
    }

    fun close() {

        if (detector != null) {
            detector!!.run_state = false
            try {
                detector!!.close()
                detector = null
                Log.d("종료", "=====================run.close======================")
            } catch (e: Exception) {
                Log.e("aaaaaaaaaaaaaaaaa", e.toString())
            }

        }
    }

    fun build(mWidth: Int, mHeight: Int) {

        previewWidth = mWidth
        previewHeight = mHeight

        // 90,270 이 가로
        sensorOrientation = 0
        oriBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        if (BuildConfig.DEBUG) {
            tracker = MultiBoxTracker(context)
            tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)

            ori_tracker = MultiBoxTracker(context)
            ori_tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
        }

        val cropSize_w: Int = BuildConfig.MODEL_INPUT_SIZE_W
        val cropSize_h: Int = BuildConfig.MODEL_INPUT_SIZE_H

        croppedBitmap = Bitmap.createBitmap(cropSize_w, cropSize_h, Bitmap.Config.ARGB_8888)


        ori_frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth,
            previewHeight,
            previewWidth,
            previewHeight,
            sensorOrientation!!,
            false
        )

        frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth,
            previewHeight,
            cropSize_w,
            cropSize_h,
            sensorOrientation!!,
            false
        )

        cropToFrameTransform = ImageUtils.getTransformationMatrix(
            previewWidth,
            previewHeight,
            cropSize_w,
            cropSize_h,
            sensorOrientation!!,
            false
        )

        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
            cropSize_w,
            cropSize_h,
            previewWidth,
            previewHeight,
            sensorOrientation!!,
            false
        )

    }

    fun get_results_bitmap(bitmap: Bitmap): MutableList<Recognition> {
        var res = mutableListOf<Recognition>()
        if (croppedBitmap != null && oriBitmap != null) {
            val canvas = Canvas(croppedBitmap!!)
            val ori_canvas = Canvas(oriBitmap!!)



            ori_canvas.drawBitmap(
                bitmap,
                ori_frameToCropTransform!!,
                null
            )
            canvas.drawBitmap(
                bitmap,
                cropToFrameTransform!!,
                null
            )

            val results: List<Recognition?>? =
                detector!!.recognizeImage(croppedBitmap!!)
            Log.d("예측결과", results.toString())

            ++timestamp
            val currTimestamp: Long = timestamp
            if (results!!.isNotEmpty()) {
                res = mutableListOf<Recognition>()
                for (result2 in results) {
                    var result = result2!!

                    val location = result.getLocation()
                    val bbox = RectF()
                    frameToCanvasMatrix!!.mapRect(bbox, location)
                    res.add(result)
                    res.get(res.size - 1).setLocation(bbox)
                }

            }
        } else {
            Log.e("예측결과-res", "널포인트")
        }


        Log.d("예측결과-res", res.toString())

        return res
    }


}
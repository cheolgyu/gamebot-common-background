package com.highserpot.tf.tflite

import android.graphics.Bitmap
import android.graphics.RectF
import org.json.JSONObject
import java.util.*

interface Classifier {
    abstract var run_state: Boolean

    fun recognizeImage(bitmap: Bitmap?): ArrayList<Recognition>?

    //fun get_label_condition(detections :ArrayList<Recognition>): String?
    //fun get_data_condition(detections :ArrayList<Recognition>): Recognition
    fun enableStatLogging(debug: Boolean)
    fun getStatString(): String?
    fun close()
    fun setNumThreads(num_threads: Int)
    fun setUseNNAPI(isChecked: Boolean)

    class Recognition(
        val lb: JSONObject,
        val title: String,
        val confidence: Float,
        private var location: RectF,
        var detectedClass: Int,
        //루프중 이번 인식목록의 글릭가능 가능여부는 인식목록 안에 있는 객체의 click에 저장함.
        var click: Boolean
    ) {

        fun chk_score(): Boolean {
            var res = if (lb.getInt("min") < (confidence * 100.0f).toInt()) {
                true
            } else {
                false
            }
            return res
        }

        fun getLocation(): RectF {
            return RectF(location)
        }

        fun setLocation(location: RectF) {
            this.location = location
        }

        override fun toString(): String {
            var resultString = ""
            if (lb != null) {
                resultString += "[$lb] "
            }
            resultString += "click=${click},"
            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f)
            }
            if (location != null) {
                resultString += location.toString() + " "
            }
            return resultString.trim { it <= ' ' }
        }


    }
}
package com.highserpot.tf.tflite

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.util.Log
import com.highserpot.background.BuildConfig
import com.highserpot.background.service.BackgroundServiceMP.Companion.history
import com.highserpot.background.service.BackgroundServiceMP.Companion.lastProcessingTimeMs
import com.highserpot.tf.env.LabelInfo
import com.highserpot.tf.env.Utils
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.Comparator

class YoloV4Classifier(override var run_state: Boolean) : Classifier {
    override fun enableStatLogging(logStats: Boolean) {}
    override fun getStatString(): String {
        return ""
    }

    override fun close() {
        tfLite?.close()
    }

    override fun setNumThreads(num_threads: Int) {
        if (tfLite != null) tfLite!!.setNumThreads(num_threads)
    }

    override fun setUseNNAPI(isChecked: Boolean) {
        false
    }

    private var isModelQuantized = false

    // Config values.
    // Pre-allocated buffers.
    private lateinit var labels: Vector<String>
    private lateinit var label_info: JSONArray
    private lateinit var intValues: IntArray
    private var imgData: ByteBuffer? = null
    private var tfLite: Interpreter? = null
    val OBJECT_THRESH = 0.01F

    //non maximum suppression
    protected fun nms(list: ArrayList<Classifier.Recognition>): ArrayList<Classifier.Recognition> {
        val nmsList =
            ArrayList<Classifier.Recognition>()
        for (k in labels.indices) {
            //1.find max confidence per class
            val pq =
                PriorityQueue(
                    50,
                    Comparator<Classifier.Recognition> { lhs, rhs -> // Intentionally reversed to put high confidence at the head of the queue.
                        java.lang.Float.compare(rhs.confidence, lhs.confidence)
                    })
            for (i in list.indices) {
                if (list[i].detectedClass == k) {
                    pq.add(list[i])
                }
            }

            //2.do non maximum suppression
            while (pq.size > 0) {
                //insert detection with max confidence
                val a =
                    arrayOfNulls<Classifier.Recognition>(pq.size)
                val detections: Array<Classifier.Recognition> =
                    pq.toArray(a)
                val max = detections[0]
                nmsList.add(max)
                pq.clear()
                for (j in 1 until detections.size) {
                    val detection =
                        detections[j]
                    val b = detection.getLocation()
                    if (box_iou(max.getLocation(), b) < mNmsThresh) {
                        pq.add(detection)
                    }
                }
            }
        }
        return nmsList
    }

    protected var mNmsThresh = 0.6f
    protected fun box_iou(a: RectF, b: RectF): Float {
        return box_intersection(a, b) / box_union(a, b)
    }

    protected fun box_intersection(a: RectF, b: RectF): Float {
        val w = overlap(
            (a.left + a.right) / 2, a.right - a.left,
            (b.left + b.right) / 2, b.right - b.left
        )
        val h = overlap(
            (a.top + a.bottom) / 2, a.bottom - a.top,
            (b.top + b.bottom) / 2, b.bottom - b.top
        )
        return if (w < 0 || h < 0) 0F else w * h
    }

    protected fun box_union(a: RectF, b: RectF): Float {
        val i = box_intersection(a, b)
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    protected fun overlap(
        x1: Float,
        w1: Float,
        x2: Float,
        w2: Float
    ): Float {
        val l1 = x1 - w1 / 2
        val l2 = x2 - w2 / 2
        val left = if (l1 > l2) l1 else l2
        val r1 = x1 + w1 / 2
        val r2 = x2 + w2 / 2
        val right = if (r1 < r2) r1 else r2
        return right - left
    }

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    protected fun convertBitmapToByteBuffer(bitmap: Bitmap?): ByteBuffer {
        val byteBuffer =
            ByteBuffer.allocateDirect(4 * BATCH_SIZE * INPUT_SIZE_W * INPUT_SIZE_H * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues =
            IntArray(INPUT_SIZE_W * INPUT_SIZE_H)
        bitmap!!.getPixels(
            intValues,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        )
        var pixel = 0
        for (i in 0 until INPUT_SIZE_W) {
            for (j in 0 until INPUT_SIZE_H) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat((`val` shr 16 and 0xFF) / 255.0f)
                byteBuffer.putFloat((`val` shr 8 and 0xFF) / 255.0f)
                byteBuffer.putFloat((`val` and 0xFF) / 255.0f)
            }
        }
        return byteBuffer
    }


    private fun getDetectionsForTiny(
        byteBuffer: ByteBuffer,
        bitmap: Bitmap
    ): ArrayList<Classifier.Recognition> {
        var detections =
            ArrayList<Classifier.Recognition>()
        val outputMap: MutableMap<Int, Any> =
            HashMap()
        outputMap[0] = Array(
            1
        ) {
            Array(
                OUTPUT_WIDTH_TINY[0]
            ) { FloatArray(4) }
        }
        outputMap[1] = Array(
            1
        ) {
            Array(
                OUTPUT_WIDTH_TINY[1]
            ) { FloatArray(labels.size) }
        }
        val inputArray = arrayOf<Any>(byteBuffer)
        if (tfLite != null && run_state) {
            tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        }
        val gridWidth = OUTPUT_WIDTH_TINY[0]
        val bboxes =
            outputMap[0] as Array<Array<FloatArray>>?
        val out_score =
            outputMap[1] as Array<Array<FloatArray>>?


        for (i in 0 until gridWidth) {
            var maxClass = 0f
            var detectedClass = -1
            val classes = FloatArray(labels.size)
            for (c in labels.indices) {
                classes[c] = out_score!![0][i][c]
            }
            for (c in labels.indices) {
                if (classes[c] > maxClass) {
                    detectedClass = c
                    maxClass = classes[c]
                }
            }
            val score = maxClass
            if (score > OBJECT_THRESH) {
                val xPos = bboxes!![0][i][0]
                val yPos = bboxes[0][i][1]
                val w = bboxes[0][i][2]
                val h = bboxes[0][i][3]
                val rectF = RectF(
                    Math.max(0f, xPos - w / 2),
                    Math.max(0f, yPos - h / 2),
                    Math.min(bitmap.width - 1.toFloat(), xPos + w / 2),
                    Math.min(bitmap.height - 1.toFloat(), yPos + h / 2)
                )

                if (label_info.opt(detectedClass) != null) {
                    var lb: JSONObject = label_info.get(detectedClass) as JSONObject
                    val cr = Classifier.Recognition(
                        lb,
                        lb.getString("name"),
                        score,
                        rectF,
                        detectedClass,
                        false
                    )
                    if (cr.chk_score()) {
                        detections.add(cr)
                    }
                } else {
                    Log.d("찾기", "404-detectedClass=${detectedClass}")
                }

            }
        }



        return detections
    }

    override fun recognizeImage(
        bitmap: Bitmap?
    ): ArrayList<Classifier.Recognition>? {

        if (bitmap != null && run_state) {
            val byteBuffer = convertBitmapToByteBuffer(bitmap)
            val detections: ArrayList<Classifier.Recognition>
            detections = getDetectionsForTiny(byteBuffer, bitmap)
            var detections_nms = nms(detections)
            Log.d("찾기", "전-c=${detections_nms}")
            history.set_time(lastProcessingTimeMs)
            var sort_detections = history.change_order(detections_nms)
            Log.d("찾기", "후-detections=${sort_detections}")

            return sort_detections
        } else {
            return null
        }

    }

    companion object {

        /**
         * Initializes a native TensorFlow session for classifying images.
         *
         * @param assetManager  The asset manager to be used to load assets.
         * @param modelFilename The filepath of the model GraphDef protocol buffer.
         * @param labelFilename The filepath of label file for classes.
         * @param isQuantized   Boolean representing model is quantized or not
         */
        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager,
            modelFilename: String?,
            isQuantized: Boolean
        ): Classifier {
            val d = YoloV4Classifier(true)
            d.label_info = LabelInfo.labels
            d.labels = LabelInfo.labels_vector

            try {
                val options =
                    Interpreter.Options()
                options.setNumThreads(NUM_THREADS)
                if (isNNAPI) {
                    var nnApiDelegate: NnApiDelegate? = null
                    // Initialize interpreter with NNAPI delegate for Android Pie or above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        nnApiDelegate = NnApiDelegate()
                        options.addDelegate(nnApiDelegate)
                        options.setNumThreads(NUM_THREADS)
                        options.setUseNNAPI(false)
                        options.setAllowFp16PrecisionForFp32(true)
                        options.setAllowBufferHandleOutput(true)
                        options.setUseNNAPI(true)
                    }
                }
                if (isGPU) {
                    //val gpuDelegate = GpuDelegate()
                    //options.addDelegate(gpuDelegate)
                }
                d.tfLite = Interpreter(
                    Utils.loadModelFile(
                        assetManager,
                        modelFilename
                    ), options
                )
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            d.isModelQuantized = isQuantized
            // Pre-allocate buffers.
            val numBytesPerChannel: Int
            numBytesPerChannel = if (isQuantized) {
                1 // Quantized
            } else {
                4 // Floating point
            }
            d.imgData =
                ByteBuffer.allocateDirect(1 * INPUT_SIZE_W * INPUT_SIZE_H * 3 * numBytesPerChannel)
            d.imgData!!.order(ByteOrder.nativeOrder())
            d.intValues =
                IntArray(INPUT_SIZE_W * INPUT_SIZE_H)
            return d
        }


        //config yolov4
        private const val INPUT_SIZE_W = BuildConfig.MODEL_INPUT_SIZE_W
        private const val INPUT_SIZE_H = BuildConfig.MODEL_INPUT_SIZE_H

        // Number of threads in the java app
        private const val NUM_THREADS = 4
        private const val isNNAPI = false
        private const val isGPU = false


        // config yolov4 tiny
        private val OUTPUT_WIDTH_TINY =
            intArrayOf(BuildConfig.MODEL_INPUT_ARR_SIZE, BuildConfig.MODEL_INPUT_ARR_SIZE)

        protected const val BATCH_SIZE = 1
        protected const val PIXEL_SIZE = 3
    }

}
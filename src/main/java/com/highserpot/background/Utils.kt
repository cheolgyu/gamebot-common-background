package com.highserpot.background

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.view.WindowManager
import java.io.File

class Utils(var context: Context) {

    fun get_wm_lp(wrap: Boolean): WindowManager.LayoutParams {
        val LAYOUT_FLAG: Int
        LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        var lp_item = WindowManager.LayoutParams.WRAP_CONTENT
        if (!wrap) {
            lp_item = WindowManager.LayoutParams.MATCH_PARENT
        }
        return WindowManager.LayoutParams(
            lp_item,
            lp_item,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
    }

    fun rm_full_path(full_path: String) {
        var f = File(full_path)
        if (f.exists()) {
            f.delete()
        }
    }

    fun click_xy(label_title: Int, item: RectF): FloatArray? {

        var x = item.left + (item.right - item.left) / 2 //+ add_size[0]
        var y = item.top + (item.bottom - item.top) / 2 //+  add_size[1]
        var arr = FloatArray(2)

        if (x < 0 || y < 0) {
            return null
        } else {
            arr.set(0, x)
            arr.set(1, y)
        }

        return arr
    }

    fun mkdir(): String? {
        val externalFilesDir = context.applicationContext.getExternalFilesDir(null)
        var path = ""
        if (externalFilesDir != null) {
            path =
                externalFilesDir.absolutePath + "/screenshots/"
            val storeDirectory =
                File(path)
            storeDirectory.deleteRecursively()
            if (!storeDirectory.exists()) {

                val success: Boolean = storeDirectory.mkdirs()
                if (!success) {
                    Log.e(
                        "eeee",
                        "failed to create file storage directory."
                    )
                    return null
                }
            }
            return path
        } else {
            Log.e(
                "eeee",
                "failed to create file storage directory, getExternalFilesDir is null."
            )
            return null
        }
    }
}
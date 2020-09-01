package com.highserpot.background

import android.content.Context
import android.graphics.RectF
import android.util.Log
import java.io.File

class Utils(var context: Context) {


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
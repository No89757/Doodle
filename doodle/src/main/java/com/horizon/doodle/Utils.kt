package com.horizon.doodle

import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import com.horizon.task.base.LogProxy
import com.horizon.task.lifecycle.LifeEvent
import com.horizon.task.lifecycle.LifecycleManager
import java.io.*

internal object Utils {
    private val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    private const val K = (1 shl 10).toLong()
    private const val M = (1 shl 20).toLong()
    private const val G = (1 shl 30).toLong()

    internal val context: Context by lazy {
        val ctx = DoodleContentProvider.ctx.applicationContext
        registerActivityLifeCycle(ctx)
        ctx
    }

    private fun registerActivityLifeCycle(context: Context) {
        if (context !is Application) {
            return
        }
        context.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                LifecycleManager.notify(activity, LifeEvent.SHOW)
            }

            override fun onActivityPaused(activity: Activity) {
                LifecycleManager.notify(activity, LifeEvent.HIDE)
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                LifecycleManager.notify(activity, LifeEvent.DESTROY)
            }
        })
    }

    internal val displayDimens: Point by lazy {
        fetchDimens()
    }

    val cacheDir: String by lazy {
        Utils.context.cacheDir?.path
                ?: ("/data/" + "data/" + Utils.context.packageName + "/cache")
    }

    fun formatSize(size: Long): String {
        return when {
            size >= G -> Integer.toString((size shr 30).toInt()) + '.' + (size and 0x3FFFFFFF) / (G / 10) + 'G'
            size >= M -> Integer.toString((size shr 20).toInt()) + '.' + (size and 0xFFFFF) / (M / 10) + 'M'
            size >= K -> java.lang.Long.toString(size shr 10) + 'k'
            else -> java.lang.Long.toString(size) + 'B'
        }
    }

    private fun fetchDimens(): Point {
        try {
            val windowManager = Utils.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            if (windowManager != null) {
                val point = Point()
                windowManager.defaultDisplay.getSize(point)
                if (point.x > 0 && point.y > 0) {
                    return point
                }
            }
        } catch (ignore: Exception) {
        }
        // should not be here, just in case
        return Point(1080, 1920)
    }

    fun isParamsValid(params: ViewGroup.LayoutParams?): Boolean {
        return (params != null
                && (params.width > 0 || params.width == ViewGroup.LayoutParams.WRAP_CONTENT)
                && (params.height > 0 || params.height == ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    fun getBytesCount(bitmap: Bitmap?): Int {
        return if (bitmap != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                bitmap.allocationByteCount
            } else {
                bitmap.rowBytes * bitmap.height
            }
        } else 0
    }

    fun isEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    fun closeQuietly(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: IOException) {
                // ignore
            }
        }
    }

    fun pickActivity(view: ImageView): Activity? {
        var context = view.context
        if (context is Activity) {
            return context
        }
        if (context is ContextWrapper && context.baseContext is Activity) {
            return context.baseContext as Activity
        }
        context = view.rootView.context
        return context as? Activity
    }

    fun toUriPath(resID: Int): String {
        if (resID != 0) {
            try {
                val resources = Utils.context.resources
                return (ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                        + resources.getResourcePackageName(resID) + "/"
                        + resources.getResourceTypeName(resID) + "/"
                        + resources.getResourceEntryName(resID))
            } catch (e: Exception) {
                LogProxy.e("Utils", e)
            }
        }
        return ""
    }

    fun long2Hex(num: Long): String {
        var a = num
        val buf = CharArray(16)
        for (i in 7 downTo 0) {
            val index = i shl 1
            val b = (a and 0xFF).toInt()
            buf[index] = HEX_DIGITS[b and 0xF0 shr 4]
            buf[index + 1] = HEX_DIGITS[b and 0xF]
            a = a.ushr(8)
        }
        return String(buf)
    }

    private const val ZERO = '0'.toByte()
    private const val NINE = '9'.toByte()
    private const val A = 'a'.toByte()
    private const val F = 'f'.toByte()

    private fun byte2Int(b: Byte): Int {
        return when (b) {
            in ZERO..NINE -> b - ZERO
            in A..F -> b - A + 10
            else -> throw NumberFormatException("invalid hex number")
        }
    }

    fun hex2Long(hex: String): Long {
        val buf = hex.toByteArray()
        if (buf.size != 16) {
            throw NumberFormatException("invalid long")
        }
        var a: Long = 0
        for (i in 0..7) {
            val index = i shl 1
            val b = (byte2Int(buf[index]) shl 4) or byte2Int(buf[index + 1])
            a = a shl 8
            a = a or b.toLong()
        }
        return a
    }

    /**
     * check if file exists, create new file if not.
     * if there is a directory with a name same as the file, return false.
     */
    @Throws(IOException::class)
    fun makeFileIfNotExist(file: File?): Boolean {
        return when {
            file == null -> false
            file.isFile -> true
            else -> {
                val parent = file.parentFile
                parent != null && (parent.isDirectory || parent.mkdirs()) && file.createNewFile()
            }
        }
    }

    @Throws(IOException::class)
    fun copyFile(src: File?, des: File): Boolean {
        if (src == null) {
            return false
        }
        return streamToFile(FileInputStream(src), des)
    }

    @Throws(IOException::class)
    fun streamToFile(inputStream : InputStream?, des: File): Boolean{
        if(inputStream == null || !makeFileIfNotExist(des)){
            return false
        }
        val buffer = ByteArrayPool.basicArray
        val out = FileOutputStream(des)
        try {
            while (true) {
                val count = inputStream.read(buffer)
                if (count <= 0) break
                out.write(buffer, 0, count)
            }
        }finally {
            ByteArrayPool.recycleBasicArray(buffer)
            closeQuietly(inputStream)
            closeQuietly(out)
        }
        return true
    }
}

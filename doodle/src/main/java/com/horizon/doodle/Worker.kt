package com.horizon.doodle

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.os.Process
import android.text.TextUtils
import android.util.Log
import android.widget.ImageView
import com.horizon.task.UITask
import com.horizon.task.base.LogProxy
import com.horizon.task.executor.LaneExecutor
import com.horizon.task.executor.PipeExecutor
import com.horizon.task.executor.TaskExecutor
import java.io.File
import java.io.InterruptedIOException
import java.lang.ref.WeakReference

internal class Worker(private val request: Request, imageView: ImageView?) : UITask<Void, Void, Any>() {
    private val key: Long = request.key

    private var fromDiskCache = false

    override val executor: TaskExecutor
        get() = loadingExecutor

    private val target: ImageView?
        get() {
            if (request.targetReference != null) {
                val imageView = request.targetReference!!.get()
                if (imageView != null && imageView.tag === request) {
                    return imageView
                }
            }
            return null
        }

    init {
        if (imageView != null) {
            request.workerReference = WeakReference(this)
            imageView.tag = request
        }
    }

    override fun generateTag(): String {
        val path = request.path
        return if (path.startsWith("http")) {
            TAG + path
        } else {
            Utils.long2Hex(key)
        }
    }

    override fun doInBackground(vararg params: Void): Any? {
        var bitmap: Bitmap? = null
        var source: Source? = null
        try {
            // check if target missed
            if (request.targetReference != null && target == null) {
                return null
            }

            bitmap = MemoryCache.getBitmap(key)
            if (bitmap == null) {
                MemoryCache.checkMemory()

                val filePath = DiskCache[key]
                fromDiskCache = !TextUtils.isEmpty(filePath)
                source = if (fromDiskCache) Source.valueOf(File(filePath!!)) else Source.parse(request)

                val gifDecoder = Config.gifDecoder
                if (!fromDiskCache && request.gifPriority && gifDecoder != null
                        && HeaderParser.isGif(source.magic)) {
                    return gifDecoder.decode(source.data)
                }

                bitmap = Decoder.decode(source, request, fromDiskCache)
                bitmap = transform(request, bitmap)
                if (bitmap != null) {
                    if (request.memoryCacheStrategy != MemoryCacheStrategy.NONE) {
                        val toWeakCache = request.memoryCacheStrategy == MemoryCacheStrategy.WEAK
                        MemoryCache.putBitmap(key, bitmap, toWeakCache)
                    }
                    if (!fromDiskCache && request.diskCacheStrategy and DiskCacheStrategy.RESULT != 0) {
                        storeResult(key, bitmap)
                    }
                }
            }
            return bitmap
        } catch (e: InterruptedIOException) {
            if (LogProxy.isDebug) {
                Log.d(TAG, "loading cancel")
            }
        } catch (e: InterruptedException) {
            if (LogProxy.isDebug) {
                Log.d(TAG, "loading cancel")
            }
        } catch (e: Throwable) {
            LogProxy.e(TAG, e)
        } finally {
            if (fromDiskCache && bitmap == null) {
                DiskCache.delete(request.key)
            }
            Utils.closeQuietly(source)
        }
        return null
    }

    override fun onCancelled() {
        val imageView = target
        if (imageView != null) {
            imageView.tag = null
        }
        request.simpleTarget = null
        request.callback = null
        Dispatcher.feedback(request, null, null, false)
    }

    override fun onPostExecute(result: Any?) {
        val imageView = target
        if (imageView != null) {
            imageView.tag = null
        }
        Dispatcher.feedback(request, imageView, result, false)
    }

    private fun transform(request: Request, source: Bitmap?): Bitmap? {
        var output = source
        if (output != null && !fromDiskCache && !Utils.isEmpty(request.transformations)) {
            for (transformation in request.transformations!!) {
                output = transformation.transform(output!!)
                if (output == null) {
                    break
                }
            }
        }
        return output
    }

    companion object {
        private val cpuCount = Runtime.getRuntime().availableProcessors()
        private val windowSize = Math.max(4, Math.min(cpuCount + 1, 6))
        private val loadingExecutor = LaneExecutor(PipeExecutor(windowSize))
        private val storageExecutor = PipeExecutor(1)

        private fun storeResult(key: Long, bitmap: Bitmap) {
            storageExecutor.execute {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    DiskCache.put(key, bitmap)
                } catch (e: Exception) {
                    LogProxy.e("Worker", e)
                }
            }
        }
    }
}


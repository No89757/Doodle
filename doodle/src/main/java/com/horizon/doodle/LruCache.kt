package com.horizon.doodle

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import java.util.*


internal object LruCache  {
    private val cache = LinkedHashMap<Long, BitmapWrapper>(16, 0.75f, true)
    private var sum: Long = 0
    private val minSize: Long = Runtime.getRuntime().maxMemory() / 32

    @Synchronized
    operator fun get(key: Long?): Bitmap? {
        return cache[key]?.bitmap
    }

    @Synchronized
    fun put(key: Long, bitmap: Bitmap?) {
        val capacity = Config.memoryCacheCapacity
        if (bitmap == null || capacity <= 0) {
            return
        }
        var wrapper: BitmapWrapper? = cache[key]
        if (wrapper == null) {
            wrapper = BitmapWrapper(bitmap)
            cache[key] = wrapper
            sum += wrapper.bytesCount.toLong()
            if (sum > capacity) {
                trimToSize(capacity * 9 / 10)
            }
        }
    }
    @Synchronized
    fun clearMemory() {
        trimToSize(0)
    }

    @Synchronized
    fun trimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            trimToSize(0)
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            trimToSize(Math.max(sum shr 1, minSize))
        }
    }

    private fun trimToSize(size: Long) {
        val iterator = cache.entries.iterator()
        while (iterator.hasNext() && sum > size) {
            val entry = iterator.next()
            val wrapper = entry.value
            WeakCache.put(entry.key, wrapper.bitmap)
            iterator.remove()
            sum -= wrapper.bytesCount.toLong()
        }
    }
}


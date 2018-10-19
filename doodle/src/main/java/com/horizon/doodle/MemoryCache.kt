package com.horizon.doodle


import android.content.ComponentCallbacks2
import android.graphics.Bitmap

import java.util.concurrent.atomic.AtomicBoolean

/**
 * manager of [LruCache] and [WeakCache],
 * gather some common operation.
 */
internal object MemoryCache {
    private val maxMemory = Runtime.getRuntime().maxMemory()
    private const val LOW_MEMORY = (8 shl 20).toLong()
    private const val CRITICAL_MEMORY = (3 shl 20).toLong()

    private val checking = AtomicBoolean(false)

    fun getBitmap(key: Long): Bitmap? {
        var bitmap = LruCache[key]
        if (bitmap == null) {
            bitmap = WeakCache[key]
        }
        return bitmap
    }

    fun putBitmap(key: Long, bitmap: Bitmap, toWeakCache: Boolean) {
        if (toWeakCache) {
            WeakCache.put(key, bitmap)
        } else {
            LruCache.put(key, bitmap)
        }
    }

    fun checkMemory() {
        if (checking.compareAndSet(false, true)) {
            val runtime = Runtime.getRuntime()
            val remaining = maxMemory - runtime.totalMemory() + runtime.freeMemory()
            if (remaining < CRITICAL_MEMORY) {
                LruCache.clearMemory()
            } else if (remaining < LOW_MEMORY) {
                LruCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            }
            checking.set(false)
        }
    }
}

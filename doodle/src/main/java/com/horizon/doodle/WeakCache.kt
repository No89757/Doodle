package com.horizon.doodle

import android.graphics.Bitmap
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.*

/**
 * Weak Reference to hold bitmaps, be able to reuse them before gc.
 */
internal object WeakCache {
    private val cache = HashMap<Long, BitmapWeakReference>()
    private val queue = ReferenceQueue<Bitmap>()

    private class BitmapWeakReference internal constructor(
            internal val key: Long,
            bitmap: Bitmap,
            q: ReferenceQueue<Bitmap>) : WeakReference<Bitmap>(bitmap, q)

    private fun cleanQueue() {
        var ref: BitmapWeakReference? = queue.poll() as BitmapWeakReference?
        while (ref != null) {
            cache.remove(ref.key)
            ref = queue.poll() as BitmapWeakReference?
        }
    }

    @Synchronized
    operator fun get(key: Long?): Bitmap? {
        cleanQueue()
        return cache[key]?.get()
    }

    @Synchronized
    fun put(key: Long, bitmap: Bitmap?) {
        if (bitmap != null) {
            cleanQueue()
            if (cache[key] == null) {
                cache[key] = BitmapWeakReference(key, bitmap, queue)
            }
        }
    }
}

package com.horizon.doodle

import android.util.SparseArray
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.*


internal object ByteArrayPool {
    // max array size, 128M
    private const val MAX_SIZE = 1 shl 27

    private const val BASIC_ARRAY_SIZE = 8192
    private const val MIN_BUFFER_SIZE = BASIC_ARRAY_SIZE * 2

    private const val CORE_SIZE = 10
    private var coreCount = 0
    private val coreArrays = arrayOfNulls<ByteArray>(CORE_SIZE)
    private val weakArrays = LinkedList<WeakReference<ByteArray>>()

    private val BUFFER_ARRAYS = SparseArray<LinkedList<WeakReference<ByteArray>>?>()

    val basicArray: ByteArray
        get() {
            synchronized(coreArrays) {
                if (coreCount > 0) {
                    val bytes = coreArrays[--coreCount]
                    coreArrays[coreCount] = null
                    return bytes!!
                }
                if (!weakArrays.isEmpty()) {
                    val bytes = getBytes(weakArrays)
                    if (bytes != null) {
                        return bytes
                    }
                }
            }
            return ByteArray(BASIC_ARRAY_SIZE)
        }

    fun recycleBasicArray(bytes: ByteArray?) {
        if (bytes == null) {
            return
        }
        synchronized(coreArrays) {
            if (coreCount < CORE_SIZE) {
                coreArrays[coreCount++] = bytes
            } else {
                weakArrays.add(WeakReference(bytes))
            }
        }
    }

    /**
     * Round up to power of two
     *
     * copy from [java.util.HashMap]
     */
    private fun roundPowerTwo(cap: Int): Int {
        var n = cap - 1
        n = n or n.ushr(1)
        n = n or n.ushr(2)
        n = n or n.ushr(4)
        n = n or n.ushr(8)
        n = n or n.ushr(16)
        return if (n < 0) 1 else n + 1
    }

    fun getArray(length: Int): ByteArray {
        var len = length
        if (len <= BASIC_ARRAY_SIZE) {
            return basicArray
        }
        if (len <= MIN_BUFFER_SIZE) {
            len = MIN_BUFFER_SIZE
        } else {
            len = roundPowerTwo(len)
            len = if (len in 1..(MAX_SIZE - 1)) len else MAX_SIZE
        }
        synchronized(BUFFER_ARRAYS) {
            val size = BUFFER_ARRAYS.size()
            var index = BUFFER_ARRAYS.indexOfKey(len)
            if (index < 0) {
                index = index.inv()
            }
            for (i in index until size) {
                val list = BUFFER_ARRAYS.valueAt(i)
                if (list != null && !list.isEmpty()) {
                    val bytes = getBytes(list)
                    if (bytes != null) {
                        return bytes
                    }
                }
            }
        }
        return ByteArray(len)
    }

    fun recycleArray(bytes: ByteArray?) {
        if (bytes == null) {
            return
        }
        val len = bytes.size
        if (len == BASIC_ARRAY_SIZE) {
            recycleBasicArray(bytes)
            return
        }
        synchronized(BUFFER_ARRAYS) {
            var list: LinkedList<WeakReference<ByteArray>>? = BUFFER_ARRAYS.get(len)
            if (list == null) {
                list = LinkedList()
                BUFFER_ARRAYS.put(len, list)
            }
            list.add(WeakReference(bytes))
        }
    }

    private fun getBytes(list: LinkedList<WeakReference<ByteArray>>): ByteArray? {
        val it = list.iterator()
        while (it.hasNext()) {
            val ref = it.next()
            it.remove()
            val bytes = ref.get()
            if (bytes != null) {
                return bytes
            }
        }
        return null
    }

    @Throws(IOException::class)
    fun loadData(stream: InputStream): ByteArray {
        var buffer = getArray(Math.max(MIN_BUFFER_SIZE, stream.available()))
        var bufLen = buffer.size
        var off = 0
        var count: Int
        try {
            while (true) {
                count = stream.read(buffer, off, Math.min(bufLen - off, BASIC_ARRAY_SIZE))
                if (count == -1) break
                off += count
                if (off == bufLen) {
                    if (bufLen >= MAX_SIZE) {
                        throw IOException("Required buffer too large")
                    }
                    val oldBuffer = buffer
                    buffer = buffer.copyOf(bufLen shl 1)
                    bufLen = buffer.size
                    recycleArray(oldBuffer)
                }
            }
            return buffer.copyOf(off)
        } finally {
            recycleArray(buffer)
            if (!stream.markSupported()) {
                Utils.closeQuietly(stream)
            }
        }
    }
}

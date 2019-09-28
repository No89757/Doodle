package com.horizon.doodle


import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.horizon.task.base.LogProxy
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

/**
 * Manager of [DiskCacheStrategy.RESULT] file and journal.
 *
 * Journal pattern in file like [hash1|accessTime1|hash2|accessTime2|……].
 * If accessTime is zero, means that file has been deleted.
 * With the journal, we can achieve LRU strategy and delete files out of [Config.diskCacheMaxAge].
 * And we can update accessTime randomly for we know the offset of every [JournalValue]
 */
@SuppressLint("UseSparseArrays")
internal object DiskCache {
    private const val TAG = "DiskCache"
    private const val JOURNAL_NAME = "journal"
    private const val PAGE_SIZE = 4096L

    private var sum: Long = 0
    private lateinit var cachePath: String

    private lateinit var channel: FileChannel
    private lateinit var buffer: MappedByteBuffer
    private var journalEnd: Int = 0

    private val journals: HashMap<Long, JournalValue> by lazy {
        val path = Config.diskCachePath
        cachePath = if (path.isEmpty()) {
            Utils.cacheDir + "/doodle/result/"
        } else {
            val c = path[path.length - 1]
            if (c == '/') path else "$path/"
        }
        //  HashMap is faster than LongSparseArray
        //  when cache files grow up to more than a thousand,
        HashMap<Long, JournalValue>().apply {
            try {
                readJournal(this)
                checkFiles(this)
            } catch (e: Exception) {
                LogProxy.e(TAG, e)
            }
        }
    }

    /**
     * get file path of image
     *
     * @param key key of image
     * @return file path of image, null if not exist
     */
    operator fun get(key: Long): String? {
        var value: JournalValue?
        synchronized(this) {
            value = journals[key]?.apply {
                accessTime = System.currentTimeMillis()
                buffer.putLong(offset, accessTime)
            }
        }
        return if (value != null) keyToPath(key) else null
    }

    fun put(key: Long, bitmap: Bitmap) {
        if (Config.diskCacheCapacity <= 0) {
            return
        }
        var value: JournalValue?
        synchronized(this) {
            value = journals[key]
        }
        if (value == null) {
            try {
                val name = keyToPath(key)
                // we save to temp file  and rename after saving,
                // in case the process exits when saving the bitmap
                val tmpFile = File("$name.tmp")
                if (Utils.makeFileIfNotExist(tmpFile)) {
                    saveBitmap(tmpFile, bitmap)
                    val fileLen = tmpFile.length()
                    if (!tmpFile.renameTo(File(name))) {
                        return
                    }
                    synchronized(this) {
                        addJournal(key, fileLen)
                        checkSize()
                    }
                }
            } catch (e: Exception) {
                LogProxy.e(TAG, e)
            }
        }
    }

    fun delete(key: Long) {
        synchronized(this) {
            val value = journals[key]
            if (value != null) {
                journals.remove(key)
                try {
                    buffer.putLong(value.offset, 0L)
                    File(keyToPath(key)).delete()
                } catch (e: Exception) {
                    LogProxy.e(TAG, e)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun saveBitmap(file: File, bitmap: Bitmap) {
        val out = FileOutputStream(file)
        try {
            bitmap.compress(Config.compressFormat, 100, out)
            out.flush()
        } finally {
            Utils.closeQuietly(out)
        }
    }

    @Throws(IOException::class)
    private fun checkSize() {
        if (sum > Config.diskCacheCapacity) {
            val journalList = ArrayList(journals.values)
            journalList.sort()

            // trim size to 80% of capacity (or less)
            val trimToSize = Config.diskCacheCapacity * 4 / 5
            val count = journalList.size
            var i = 0
            while (i < count && sum > trimToSize) {
                val value = journalList[i++]
                if (File(keyToPath(value.key)).delete()) {
                    journals.remove(value.key)
                    sum -= value.fileLen
                }
            }

            alignFile(journalList, i)
        }
    }

    private fun alignFile(journalList: ArrayList<JournalValue>, start: Int) {
        val count = journalList.size
        var i = start

        val newLen = alignLength(((count - i) * 16).toLong())
        if (newLen < buffer.capacity()) {
            buffer.force()
            channel.truncate(newLen)
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, newLen)
        }
        buffer.clear()
        while (i < count) {
            val value = journalList[i++]
            buffer.putLong(value.key)
            value.offset = buffer.position()
            buffer.putLong(value.accessTime)
        }
        journalEnd = buffer.position()

        while (buffer.hasRemaining()) {
            buffer.putLong(0L)
        }
    }

    @Throws(IOException::class)
    private fun checkFiles(map: HashMap<Long, JournalValue>) {
        val cacheDir = File(cachePath)
        if (!cacheDir.isDirectory) {
            return
        }

        val files = cacheDir.listFiles()
        if (files == null || files.isEmpty()) {
            return
        }

        val now = System.currentTimeMillis()
        val fileSet = HashSet<Long>()

        for (file in files) {
            val name = file.name
            if (JOURNAL_NAME == name || !file.isFile) {
                continue
            }
            val key = nameToKey(name)
            if (key != null) {
                val fileLen = file.length()
                val value = map[key]
                if (value == null) {
                    addJournal(key, fileLen)
                } else if (now - value.accessTime > Config.diskCacheMaxAge) {
                    map.remove(key)
                    file.delete()
                    continue
                } else {
                    value.fileLen = fileLen
                    sum += fileLen
                }
                fileSet.add(key)
            } else {
                file.delete()
            }
        }
        map.keys.retainAll(fileSet)
    }

    @Throws(IOException::class)
    private fun readJournal(map: HashMap<Long, JournalValue>) {
        val journalFile = File(cachePath + JOURNAL_NAME)
        if (Utils.makeFileIfNotExist(journalFile)) {
            var invalidCount = 0
            val accessFile = RandomAccessFile(journalFile, "rw")
            val length = alignLength(accessFile.length())
            channel = accessFile.channel
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, length)
            buffer.position(0)
            while (buffer.hasRemaining()) {
                val key = buffer.long
                if (key == 0L) {
                    journalEnd = buffer.position() - 8
                    break
                }
                val accessTime = buffer.long
                if (accessTime > 0) {
                    map[key] = JournalValue(key, accessTime, 0, buffer.position() - 8)
                } else {
                    invalidCount++
                }
            }

            if (buffer.position() == buffer.capacity()) {
                journalEnd = buffer.position()
            }

            if (invalidCount * 16 >= PAGE_SIZE) {
                // make gc for journal
                alignFile(ArrayList(map.values), 0)
            }
        }
    }

    @Throws(IOException::class)
    private fun addJournal(key: Long, fileLen: Long) {
        val now = System.currentTimeMillis()
        val offset = appendJournal(key, now)
        journals[key] = JournalValue(key, now, fileLen, offset)
        sum += fileLen
    }

    @Throws(IOException::class)
    private fun appendJournal(key: Long, accessTime: Long): Int {
        val end = journalEnd
        if (end + 16 > buffer.capacity()) {
            buffer.force()
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, (end + PAGE_SIZE))
        }
        buffer.position(end)
        buffer.putLong(key)
        buffer.putLong(accessTime)
        journalEnd = end + 16
        return end + 8
    }

    private fun alignLength(len: Long): Long {
        return when {
            len <= 0 -> PAGE_SIZE
            len and 0xFFF != 0L -> len + PAGE_SIZE shr 12 shl 12
            else -> len
        }
    }

    private fun nameToKey(name: String): Long? {
        if (name.length == 16) {
            try {
                return Utils.hex2Long(name)
            } catch (e: NumberFormatException) {
                LogProxy.e(TAG, e)
            }
        }
        return null
    }

    private fun keyToPath(key: Long): String {
        return cachePath + Utils.long2Hex(key)
    }

    private class JournalValue internal constructor(
            internal var key: Long,
            internal var accessTime: Long,
            internal var fileLen: Long,
            internal var offset: Int) : Comparable<JournalValue> {

        override fun compareTo(other: JournalValue): Int {
            return accessTime.compareTo(other.accessTime)
        }
    }
}

package com.horizon.doodle

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * A class for parsing the exif orientation and other data from image header.
 *
 * Copy from Glide
 */
internal object HeaderParser {
    const val UNKNOWN_ORIENTATION = -1

    private const val GIF_HEADER = 0x474946
    private const val EXIF_MAGIC_NUMBER = 0xFFD8

    // "MM".
    private const val MOTOROLA_TIFF_MAGIC_NUMBER = 0x4D4D
    // "II".
    private const val INTEL_TIFF_MAGIC_NUMBER = 0x4949
    //"Exif\0\0";
    private val JPEG_EXIF_SEGMENT_PREAMBLE = byteArrayOf(69, 120, 105, 102, 0, 0)

    private const val SEGMENT_SOS = 0xDA
    private const val MARKER_EOI = 0xD9
    private const val SEGMENT_START_ID = 0xFF
    private const val EXIF_SEGMENT_TYPE = 0xE1
    private const val ORIENTATION_TAG_TYPE = 0x0112
    private val BYTES_PER_FORMAT = intArrayOf(0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8)

    fun isGif(magic: Int): Boolean {
        return magic shr 8 and 0xFFFFFF == GIF_HEADER
    }

    fun possiblyExif(magic: Int): Boolean {
        val magicNumber = magic shr 16 and 0xFFFF
        return (magicNumber and EXIF_MAGIC_NUMBER == EXIF_MAGIC_NUMBER
                || magicNumber == MOTOROLA_TIFF_MAGIC_NUMBER
                || magicNumber == INTEL_TIFF_MAGIC_NUMBER)
    }

    @Throws(IOException::class)
    fun readInt(stream: InputStream): Int {
        val ch1 = stream.read()
        val ch2 = stream.read()
        val ch3 = stream.read()
        val ch4 = stream.read()
        if ((ch1 or ch2 or ch3 or ch4) < 0)
            throw EOFException()
        return (ch1 shl 24) + (ch2 shl 16) + (ch3 shl 8) + ch4
    }

    @Throws(IOException::class)
    fun getOrientation(stream: InputStream): Int {
        var exifData: ByteArray? = null
        try {
            if (stream.markSupported()) {
                stream.mark(Integer.MAX_VALUE)
            }

            if (stream.skip(2) != 2L) {
                return UNKNOWN_ORIENTATION
            }
            val exifSegmentLength = moveToExifSegmentAndGetLength(stream)
            if (exifSegmentLength == -1) {
                return UNKNOWN_ORIENTATION
            }
            exifData = ByteArrayPool.getArray(exifSegmentLength)
            var toRead = exifSegmentLength
            var read: Int
            while (toRead > 0) {
                read = stream.read(exifData, exifSegmentLength - toRead, toRead)
                if (read == -1) break
                toRead -= read
            }
            if (toRead != 0) {
                return UNKNOWN_ORIENTATION
            }

            return if (hasJpegExifPreamble(exifData, exifSegmentLength)) {
                parseExifSegment(RandomAccessReader(exifData, exifSegmentLength))
            } else UNKNOWN_ORIENTATION
        } finally {
            ByteArrayPool.recycleArray(exifData)
            // the input stream is one of FileInputStream, AssetInputStream, RecycledInputStream,
            // and FileInputStream can just read one time, so we close it directly
            if (!stream.markSupported()) {
                Utils.closeQuietly(stream)
            } else if (stream is RecycledInputStream) {
                stream.rewind()
            } else {
                stream.reset()
            }
        }
    }

    private fun hasJpegExifPreamble(exifData: ByteArray, exifSegmentLength: Int): Boolean {
        val exifPreambleLen = JPEG_EXIF_SEGMENT_PREAMBLE.size
        if (exifSegmentLength <= exifPreambleLen) {
            return false
        }
        for (i in 0 until exifPreambleLen) {
            if (exifData[i] != JPEG_EXIF_SEGMENT_PREAMBLE[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Moves reader to the start of the exif segment and returns the length of the exif segment or
     * `-1` if no exif segment is found.
     */
    @Throws(IOException::class)
    private fun moveToExifSegmentAndGetLength(stream: InputStream): Int {
        while (true) {
            val segmentId = stream.read() and 0xFF
            if (segmentId != SEGMENT_START_ID) {
                return -1
            }

            val segmentType = stream.read() and 0xFF
            if (segmentType == SEGMENT_SOS || segmentType == MARKER_EOI) {
                return -1
            }

            // Segment length includes bytes for segment length.
            val segmentLength = (stream.read() shl 8 and 0xFF00 or (stream.read() and 0xFF)) - 2
            if (segmentType != EXIF_SEGMENT_TYPE) {
                if (skip(stream, segmentLength.toLong()) != segmentLength.toLong()) {
                    return -1
                }
            } else {
                return segmentLength
            }
        }
    }

    @Throws(IOException::class)
    private fun skip(stream: InputStream, total: Long): Long {
        if (total < 0) {
            return 0
        }

        var toSkip = total
        while (toSkip > 0) {
            val skipped = stream.skip(toSkip)
            if (skipped > 0) {
                toSkip -= skipped
            } else {
                // Skip has no specific contract as to what happens when you reach the end of
                // the stream. To differentiate between temporarily not having more data and
                // having finished the stream, we read a single byte when we fail to skip any
                // amount of data.
                val testEofByte = stream.read()
                if (testEofByte == -1) {
                    break
                } else {
                    toSkip--
                }
            }
        }
        return total - toSkip
    }

    private fun parseExifSegment(data: RandomAccessReader): Int {
        val headerOffsetSize = JPEG_EXIF_SEGMENT_PREAMBLE.size

        val byteOrderIdentifier = data.getInt16(headerOffsetSize)
        val byteOrder = if (byteOrderIdentifier.toInt() == INTEL_TIFF_MAGIC_NUMBER)
            ByteOrder.LITTLE_ENDIAN
        else
            ByteOrder.BIG_ENDIAN
        data.order(byteOrder)

        val firstIfdOffset = data.getInt32(headerOffsetSize + 4) + headerOffsetSize
        val tagCount = data.getInt16(firstIfdOffset).toInt()
        for (i in 0 until tagCount) {
            val tagOffset = calcTagOffset(firstIfdOffset, i)

            val tagType = data.getInt16(tagOffset).toInt()
            // We only want orientation.
            if (tagType != ORIENTATION_TAG_TYPE) {
                continue
            }

            val formatCode = data.getInt16(tagOffset + 2).toInt()
            // 12 is max format code.
            if (formatCode < 1 || formatCode > 12) {
                continue
            }

            val componentCount = data.getInt32(tagOffset + 4)
            if (componentCount < 0) {
                continue
            }

            val byteCount = componentCount + BYTES_PER_FORMAT[formatCode]
            if (byteCount > 4) {
                continue
            }

            val tagValueOffset = tagOffset + 8
            if (tagValueOffset < 0 || tagValueOffset > data.length()) {
                continue
            }

            if (byteCount < 0 || tagValueOffset + byteCount > data.length()) {
                continue
            }

            //assume componentCount == 1 && fmtCode == 3
            return data.getInt16(tagValueOffset).toInt()
        }

        return -1
    }

    private fun calcTagOffset(offset: Int, tagIndex: Int): Int {
        return offset + 2 + 12 * tagIndex
    }

    private class RandomAccessReader internal constructor(data: ByteArray, length: Int) {
        private val data: ByteBuffer = ByteBuffer.wrap(data)
                .order(ByteOrder.BIG_ENDIAN)
                .limit(length) as ByteBuffer

        internal fun order(byteOrder: ByteOrder) {
            this.data.order(byteOrder)
        }

        internal fun length(): Int {
            return data.remaining()
        }

        internal fun getInt32(offset: Int): Int {
            return if (isAvailable(offset, 4)) data.getInt(offset) else -1
        }

        internal fun getInt16(offset: Int): Short {
            return if (isAvailable(offset, 2)) data.getShort(offset) else -1
        }

        private fun isAvailable(offset: Int, byteSize: Int): Boolean {
            return data.remaining() - offset >= byteSize
        }
    }

}
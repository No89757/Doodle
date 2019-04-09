package com.horizon.doodle

import android.graphics.*
import android.media.ExifInterface
import android.util.Log
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import com.horizon.task.base.LogProxy
import java.io.IOException

/**
 * Bitmap Decoder
 */
internal object Decoder {
    private const val TAG = "Decoder"

    const val NO_CLIP = -1
    private const val MATRIX = 0
    private const val CENTER = 5
    private const val CENTER_CROP = 6
    const val CENTER_INSIDE = 7

    fun mapScaleType(scaleType: ScaleType): Int {
        return when (scaleType) {
            ImageView.ScaleType.MATRIX -> MATRIX
            ImageView.ScaleType.CENTER -> CENTER
            ImageView.ScaleType.CENTER_CROP -> CENTER_CROP
            else -> CENTER_INSIDE
        }
    }

    @Throws(IOException::class)
    fun decode(source: Source?, request: Request?, fromDiskCache: Boolean): Bitmap? {
        if (source == null || request == null) {
            return null
        }

        val options = BitmapFactory.Options()
        options.inScaled = false
        options.inMutable = true
        options.inPreferredConfig = request.config

        var clipType = if (fromDiskCache) NO_CLIP else request.clipType
        var bitmap: Bitmap?
        try {
            var orientation = HeaderParser.UNKNOWN_ORIENTATION
            var rotated = false
            if (!fromDiskCache && HeaderParser.possiblyExif(source.magic)) {
                orientation = source.orientation
                // orientation input [5,8] mean rotate 90 or 270 degrees
                rotated = orientation >= ExifInterface.ORIENTATION_TRANSPOSE
            }

            if (!fromDiskCache && (clipType != NO_CLIP)) {
                options.inJustDecodeBounds = true
                source.decode(options)
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    clipType = NO_CLIP
                }
                options.inJustDecodeBounds = false
            }

            val dwidth = options.outWidth
            val dheight = options.outHeight

            // if bitmap rotated 90 or 270 degrees,
            // swap view width and height to calculate target sizes
            val vwidth = if (rotated) request.viewHeight else request.viewWidth
            val vheight = if (rotated) request.viewWidth else request.viewHeight

            when (clipType) {
                NO_CLIP -> bitmap = source.decode(options)
                MATRIX -> bitmap = if (dwidth > vwidth || dheight > vheight) {
                    val right = if (dwidth > vwidth) vwidth else dwidth
                    val bottom = if (dheight > vheight) vheight else dheight
                    val rect = Rect(0, 0, right, bottom)
                    source.decodeRegion(rect, options)
                } else {
                    source.decode(options)
                }
                CENTER -> if (dwidth > vwidth || dheight > vheight) {
                    val left: Int
                    val top: Int
                    val right: Int
                    val bottom: Int
                    if (dwidth > vwidth) {
                        left = Math.round((dwidth - vwidth) * 0.5f)
                        right = Math.min(left + vwidth, dwidth)
                    } else {
                        left = 0
                        right = dwidth
                    }
                    if (dheight > vheight) {
                        top = Math.round((dheight - vheight) * 0.5f)
                        bottom = Math.min(top + vheight, dheight)
                    } else {
                        top = 0
                        bottom = dheight
                    }
                    val rect = Rect(left, top, right, bottom)
                    bitmap = source.decodeRegion(rect, options)
                } else {
                    bitmap = source.decode(options)
                }
                CENTER_CROP -> {
                    if (dwidth * vheight > vwidth * dheight) {
                        if (dheight > vheight) {
                            options.inScaled = true
                            options.inTargetDensity = vheight
                            options.inDensity = dheight
                        }
                    } else {
                        if (dwidth > vwidth) {
                            options.inScaled = true
                            options.inTargetDensity = vwidth
                            options.inDensity = dwidth
                        }
                    }
                    bitmap = source.decode(options)
                    if (bitmap != null) {
                        bitmap.density = Bitmap.DENSITY_NONE
                        val bw = bitmap.width
                        val bh = bitmap.height
                        val d = bw * vheight - vwidth * bh
                        if (d > 0) {
                            val dx = (bw - vwidth * bh / vheight.toFloat()) * 0.5f
                            bitmap = Bitmap.createBitmap(bitmap, Math.round(dx), 0, Math.round(bw - 2 * dx), bh)
                        } else if (d < 0) {
                            val dy = (bh - vheight * bw / vwidth.toFloat()) * 0.5f
                            bitmap = Bitmap.createBitmap(bitmap, 0, Math.round(dy), bw, Math.round(bh - 2 * dy))
                        }
                    }
                }
                else -> {
                    // other case，deal with centerInside
                    if (dwidth > vwidth || dheight > vheight) {
                        options.inScaled = true
                        if (dwidth * vheight > vwidth * dheight) {
                            options.inTargetDensity = vwidth
                            options.inDensity = dwidth
                        } else {
                            options.inTargetDensity = vheight
                            options.inDensity = dheight
                        }
                    }
                    bitmap = source.decode(options)
                }
            }

            if (bitmap != null && orientation >= ExifInterface.ORIENTATION_FLIP_HORIZONTAL) {
                bitmap = rotateImageExif(bitmap, orientation)
            }

            if (LogProxy.isDebug) {
                if (bitmap != null) {
                    Log.i(TAG, " allocated:" + Utils.formatSize(Utils.getBytesCount(bitmap).toLong())
                            + " v:" + request.viewWidth + "x" + request.viewHeight
                            + " d:" + dwidth + "x" + dheight
                            + " bitmap:" + bitmap.width + "x" + bitmap.height
                            + " source:" + source.javaClass.simpleName
                    )
                } else {
                    Log.e(TAG, "Decoder image failed. " + request.path)
                }
            }
        } finally {
            ByteArrayPool.recycleBasicArray(options.inTempStorage)
        }

        if (bitmap != null) {
            bitmap.density = Bitmap.DENSITY_NONE
        }

        return bitmap
    }

    // rotate image, Copy from Glide
    private fun rotateImageExif(inBitmap: Bitmap, exifOrientation: Int): Bitmap {
        val matrix = Matrix()
        initializeMatrixForRotation(exifOrientation, matrix)

        val newRect = RectF(0f, 0f, inBitmap.width.toFloat(), inBitmap.height.toFloat())
        matrix.mapRect(newRect)
        matrix.postTranslate(-newRect.left, -newRect.top)

        val newWidth = Math.round(newRect.width())
        val newHeight = Math.round(newRect.height())
        var config: Bitmap.Config? = inBitmap.config
        if (config == null) {
            config = Bitmap.Config.ARGB_8888
        }
        val result = Bitmap.createBitmap(newWidth, newHeight, config)
        val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
        val canvas = Canvas(result)
        canvas.drawBitmap(inBitmap, matrix, paint)
        canvas.setBitmap(null)
        return result
    }

    private fun initializeMatrixForRotation(exifOrientation: Int, matrix: Matrix) {
        when (exifOrientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        }// Do nothing.
    }
}

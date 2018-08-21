package com.horizon.doodle.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF


class ResizeTransformation(private val width: Int, private val height: Int) : Transformation {
    override fun transform(source: Bitmap): Bitmap? {
        if (width == source.width && height == source.height) {
            return source
        }

        val config = source.config
        val output = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val dst = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, dst, paint)
        return output
    }

    override fun key(): String {
        return "Resize" + width + "x" + height
    }
}

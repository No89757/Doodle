package com.horizon.doodle.transform

import android.graphics.*

class CircleTransformation (private val diameter: Int = 0)  : Transformation {
    override fun transform(source: Bitmap): Bitmap? {
        if (diameter < 0) {
            return source
        }
        val w = source.width
        val h = source.height
        val c = Math.min(w, h)
        val r = c shr 1
        val d = if (diameter != 0) diameter else c
        val x = w shr 1
        val y = h shr 1


        val src = Rect(x - r, y - r, x + r, y + r)
        val dst = RectF(0f, 0f, d.toFloat(), d.toFloat())

        val config = source.config
        val output = Bitmap.createBitmap(d, d, config ?: Bitmap.Config.ARGB_8888)
        output.density = source.density

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG
                or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawOval(dst, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, src, dst, paint)

        return output
    }

    override fun key(): String {
        return "Circle$diameter"
    }

}

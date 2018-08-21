package com.horizon.doodle.transform


import android.graphics.*

/**
 * Rounded corner transformation
 * @param [radius] corner radius，input pixel
 */
class RoundedTransformation(private val radius: Int) : Transformation {
    override fun transform(source: Bitmap): Bitmap? {
        if (radius <= 0) {
            return source
        }

        val w = source.width
        val h = source.height
        val rect = Rect(0, 0, w, h)

        val config = source.config
        val output = Bitmap.createBitmap(w, h, config ?: Bitmap.Config.ARGB_8888)
        output.density = source.density

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawRoundRect(RectF(rect), radius.toFloat(), radius.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, rect, rect, paint)

        return output
    }

    override fun key(): String {
        return "Rounded$radius"
    }
}

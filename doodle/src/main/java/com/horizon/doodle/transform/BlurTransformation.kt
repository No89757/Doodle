package com.horizon.doodle.transform

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.horizon.doodle.Utils

/**
 * Blur Transformation
 *
 * The [radius] supported range 0 < radius <= 25，default is 10
 */
class BlurTransformation(private val radius: Float = 10f)
    : Transformation {

    override fun transform(source: Bitmap): Bitmap? {
        val rs = RenderScript.create(Utils.context)
        val allocation = Allocation.createFromBitmap(rs, source)
        val blur = ScriptIntrinsicBlur.create(rs, allocation.element)
        blur.setInput(allocation)
        blur.setRadius(radius)
        blur.forEach(allocation)
        allocation.copyTo(source)
        rs.destroy()
        return source
    }

    override fun key(): String {
        return "Blur$radius"
    }

}
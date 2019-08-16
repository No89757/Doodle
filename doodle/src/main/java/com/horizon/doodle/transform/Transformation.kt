package com.horizon.doodle.transform


import android.graphics.Bitmap

interface Transformation {
    fun transform(source: Bitmap): Bitmap?

    /**
     * @return identify of this transformation, part of request key,
     * suggest to be "constance string" or "constance string + parameter".
     */
    fun key(): String
}
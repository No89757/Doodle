package com.horizon.doodle.interfaces

import android.graphics.Bitmap

interface SimpleTarget {
    /**
     * This method will be called when get the bitmap from cache or task finished,
     * would not be called if task canceled.
     *
     * @param bitmap result bitmap, null if loading bitmap failed
     */
    fun onComplete(bitmap: Bitmap?)
}

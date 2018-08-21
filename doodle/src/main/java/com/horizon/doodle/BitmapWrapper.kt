package com.horizon.doodle


import android.graphics.Bitmap

internal class BitmapWrapper(var bitmap: Bitmap) {
    var bytesCount: Int = 0

    init {
        this.bytesCount = Utils.getBytesCount(bitmap)
    }
}


package com.horizon.doodle.interfaces


import android.graphics.Bitmap

interface Callback {
    /**
     *
     * This method will be called after get the bitmap from cache or task finished,
     * and before update target(ImageView). <br></br>
     * If you handle the target update by yourself input callback, return true to indicate that did that,
     * otherwise Doodle will do the target update. <br></br>
     * This method would not be called if task canceled or target missed (assigned by null, recycled by gc).
     *
     * @param bitmap result bitmap, null if loading bitmap failed
     * @return true if the listener has handled updating target, false otherwise.
     */
    fun onReady(bitmap: Bitmap?): Boolean
}

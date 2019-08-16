package com.horizon.doodle.interfaces

import android.graphics.drawable.Drawable


/**
 * Doodle supply a interface for custom decoding dynamic gif.
 *
 * You can call [com.horizon.doodle.Config.setGifDecoder] to assigned the gif decoder,
 * then if the source is a gif image, it will decode by you custom decoder,
 * otherwise it will decode by [android.graphics.BitmapFactory], and get a bitmap to the target.
 * <br></br>
 * You can call [com.horizon.doodle.Request.asBitmap] if you want bitmap, no mather source is gif or other format.
 *
 * It's recommended using [android-gif-drawable](https://github.com/koral--/android-gif-drawable) to decode gif.
 *
 * If no gif decoder had been set, Doodle will just pick the first frame of gif image.
 */
interface GifDecoder {
    /**
     * decode data to drawable,
     * the method call input worker thread
     *
     * @param bytes source data
     * @return a drawable result
     * @throws Exception exception that not handled
     */
    @Throws(Exception::class)
    fun decode(bytes: ByteArray): Drawable
}

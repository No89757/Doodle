package com.horizon.doodle.interfaces

import android.graphics.drawable.Drawable


/**
 * Doodle not support decode dynamic gif,
 * so we supply a interface for custom decoding dynamic gif.
 *
 *
 * You can call [Config.Builder.setGifDecoder] to assigned the gif decoder,
 * then if the source is a gif image, it will decode by you custom decoder,
 * otherwise it will decode by [android.graphics.BitmapFactory], and get a bitmap to the target.
 * <br></br>
 * You can call [Request.asBitmap] if you want bitmap, no mather source is gif or other format.
 *
 *
 * It's unnecessary to use custom decoder if you just want the first frame.
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

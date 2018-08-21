package com.horizon.doodledemo.application

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.horizon.doodle.Doodle
import com.horizon.doodle.GifDecoder
import com.horizon.task.base.LogProxy
import pl.droidsonroids.gif.GifDrawable

internal object AppInitManager {
    fun initApplication(context: Application) {
        LogProxy.init(GlobalLogger)
        Doodle.init(context)
                .setDiskCacheCapacity(256L shl 20)
                .setMemoryCacheCapacity(128L shl 20)
                .setDefaultBitmapConfig(Bitmap.Config.ARGB_8888)
                .setGifDecoder(gifDecoder)
    }

    private val gifDecoder = object : GifDecoder {
        override fun decode(bytes: ByteArray): Drawable {
            return GifDrawable(bytes)
        }
    }
}

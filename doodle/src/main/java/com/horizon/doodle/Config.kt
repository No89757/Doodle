package com.horizon.doodle


import android.graphics.Bitmap
import com.horizon.doodle.interfaces.GifDecoder

object Config {
    internal var userAgent: String = ""
    internal var diskCachePath: String = ""
    internal var diskCacheCapacity: Long = 128L shl 20
    internal var diskCacheMaxAge: Long = 30 * 24 * 3600 * 1000L
    internal var sourceCacheCapacity: Long = 256L shl 20
    internal var memoryCacheCapacity: Long = Runtime.getRuntime().maxMemory() / 6
    internal var compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    internal var bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888
    internal var gifDecoder: GifDecoder? = null

    fun setUserAgent(userAgent: String): Config {
        this.userAgent = userAgent
        return this
    }

    fun setDiskCachePath(path: String): Config {
        this.diskCachePath = path
        return this
    }

    /**
     * We would saved result bitmap to disk if set strategy with
     * [DiskCacheStrategy.RESULT] or [DiskCacheStrategy.ALL] (default).
     *
     * @param capacity capacity of disk cache
     * @return
     */
    fun setDiskCacheCapacity(capacity: Long): Config {
        this.diskCacheCapacity = capacity
        return this
    }

    fun setDiskCacheMaxAge(maxAge: Long): Config {
        this.diskCacheMaxAge = maxAge
        return this
    }

    /**
     * We use OkHttp to get network source and cache source file,
     * OkHttp Cache need to init with capacity.
     *
     * @param capacity max size of source cache
     */
    fun setSourceCacheCapacity(capacity: Long): Config {
        this.sourceCacheCapacity = capacity
        return this
    }

    /**
     * Set capacity of [LruCache]
     */
    fun setMemoryCacheCapacity(capacity: Long): Config {
        this.memoryCacheCapacity = capacity
        return this
    }

    /**
     * Set compress format
     *
     * @param format one of [JPEG, PNG, WEBP], default is PNG
     */
    fun setCompressFormat(format: Bitmap.CompressFormat): Config {
        this.compressFormat = format
        return this
    }

    fun setDefaultBitmapConfig(config: Bitmap.Config): Config {
        this.bitmapConfig = config
        return this
    }

    fun setGifDecoder(gifDecoder: GifDecoder): Config {
        this.gifDecoder = gifDecoder
        return this
    }
}

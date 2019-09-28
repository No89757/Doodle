package com.horizon.doodle.interfaces

import java.io.File

/**
 * You can use this to cache source file to your own directories.
 */
interface CacheInterceptor {
    /**
     * @param url of image
     * @return custom cache path
     */
    fun cachePath(url: String): File?
}

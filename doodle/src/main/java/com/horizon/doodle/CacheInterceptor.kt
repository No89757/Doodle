package com.horizon.doodle

import java.io.File

/**
 * Use to manager cache by caller. <br></br>
 *
 * Note that Doodle will still keep the cache by cache strategy,
 * this interceptor just supply a copy of source file to cache by caller.
 */
interface CacheInterceptor {
    /**
     * @param url of image
     * @return custom cache path
     */
    fun cachePath(url: String): File?
}

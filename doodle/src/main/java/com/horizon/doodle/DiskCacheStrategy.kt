package com.horizon.doodle


/**
 * disk caching strategies, refer to glide v3
 */
object DiskCacheStrategy {
    /**
     * Saves no data to cache.
     */
    const val NONE = 0
    /**
     * Saves just the original data to cache，just for remote source.
     * For local source, it's not necessary to make copy to cache.
     */
    const val SOURCE = 1
    /**
     * Saves result to cache after decode and apply all transformations .
     */
    const val RESULT = 2
    /**
     * Caches with both [SOURCE] and [RESULT].
     */
    const val ALL = 3
}

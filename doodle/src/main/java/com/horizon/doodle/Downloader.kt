package com.horizon.doodle

import com.horizon.task.base.LogProxy
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.ByteString
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Http Client, need to init Context when app start,
 * to set up the cache path.
 */
internal object Downloader  {
    private const val TAG = "Downloader"

    private lateinit var cacheDirPath: String

    private val client: OkHttpClient by lazy {
        val capacity = Config.sourceCacheCapacity
        val agent = Config.userAgent
        val maxSize = if (capacity > 0) capacity else (256L shl 20)
        cacheDirPath = Utils.cacheDir + "/doodle/source/"
        val builder = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .cache(Cache(File(cacheDirPath), maxSize))
        if (!agent.isEmpty()) {
            builder.addNetworkInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", agent).build())
            }
        }
        builder.build()
    }

    @Throws(IOException::class)
    fun getSource(request: Request): Any? {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            // at OkHttp 3.12.0, we can get cached body (the file) like this,
            // if input later version failed, we need to always covert to StreamSource.
            val inputStream : InputStream? = response.body()!!.byteStream()
            if (inputStream != null && inputStream.toString().contains("FileInputStream")) {
                val cacheFile = File(cacheDirPath + Cache.key(request.url()) + ".1")
                if (cacheFile.exists()) {
                    Utils.closeQuietly(inputStream)
                    return cacheFile
                }
            }
            return inputStream
        } else {
            throw IOException("request failed, " + response.code())
        }
    }

    fun downloadOnly(url: String): File? {
        var buffer: ByteArray? = null
        var inputStream: InputStream? = null
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                inputStream = response.body()!!.byteStream()
                if (inputStream == null) {
                    return null
                }
                if (!inputStream.toString().contains("FileInputStream")) {
                    buffer = ByteArrayPool.basicArray
                    while (inputStream.read(buffer) != -1) {
                        // do nothing, just invoke read() to download -_-
                    }
                }
                val cacheFile = File(cacheDirPath + Cache.key(request.url()) + ".1")
                if (cacheFile.exists()) {
                    return cacheFile
                }
            }
        } catch (e: Exception) {
            LogProxy.e(TAG, e)
        } finally {
            ByteArrayPool.recycleBasicArray(buffer)
            Utils.closeQuietly(inputStream)
        }
        return null
    }

    fun getSourceCacheFile(url: String): File? {
        val cacheFile = File(cacheDirPath + ByteString.encodeUtf8(url).md5().hex() + ".1")
        return if (cacheFile.exists()) cacheFile else null
    }
}

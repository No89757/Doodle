package com.horizon.doodle

import com.horizon.task.base.LogProxy
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.ByteString
import java.io.*
import java.util.concurrent.TimeUnit

internal object Downloader {
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
        if (agent.isNotEmpty()) {
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
            val inputStream: InputStream? = response.body()!!.byteStream()
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

    fun downloadOnly(url: String, desFile: File? = null): File? {
        var tmpFile: File? = null
        try {
            if (desFile != null && desFile.exists()) {
                return desFile
            }

            val request = Request.Builder().url(url)
                    .cacheControl(CacheControl.Builder().noStore().build())
                    .build()

            val fileName = Cache.key(request.url()) + ".1"
            tmpFile = File(cacheDirPath, "tmp_$fileName")
            val cachedFile = File(cacheDirPath, fileName)
            if (cachedFile.exists()) {
                if (desFile == null) {
                    return cachedFile
                }
                if (Utils.copyFile(cachedFile, tmpFile) && tmpFile.renameTo(desFile)) {
                    return desFile
                }
            }

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val inputStream = response.body()!!.byteStream()
                val targetFile = desFile ?: cachedFile
                if (Utils.streamToFile(inputStream, tmpFile) && tmpFile.renameTo(targetFile)) {
                    return targetFile
                }
            }
        } catch (e: Exception) {
            LogProxy.e(TAG, e)
        } finally {
            deleteFileQuietly(tmpFile)
        }
        return null
    }

    private fun deleteFileQuietly(file: File?) {
        try {
            if (file != null && file.exists()) {
                file.delete()
            }
        } catch (ignore: Exception) {
        }
    }

    fun getSourceCacheFile(url: String): File? {
        val cacheFile = File(cacheDirPath + ByteString.encodeUtf8(url).md5().hex() + ".1")
        return if (cacheFile.exists()) cacheFile else null
    }
}

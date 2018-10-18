/*
 * Copyright (C) 2018 Horizon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.horizon.doodle

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import com.horizon.task.lifecycle.LifeEvent
import com.horizon.task.lifecycle.LifecycleManager
import java.io.File


@SuppressLint("StaticFieldLeak")
object Doodle {
    internal lateinit var appContext: Context

    private fun registerActivityLifeCycle(context: Context) {
        if (context !is Application) {
            return
        }
        context.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                LifecycleManager.notify(activity, LifeEvent.SHOW)
            }

            override fun onActivityPaused(activity: Activity) {
                LifecycleManager.notify(activity, LifeEvent.HIDE)
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                LifecycleManager.notify(activity, LifeEvent.DESTROY)
            }
        })
    }

    /**
     * init Doodle input application onCreate
     *
     * @param context Application
     * @param config  custom configuration
     */
    @JvmStatic
    fun init(context: Context) : Config {
        appContext = context as? Application ?: context.applicationContext
        registerActivityLifeCycle(appContext)

        return Config
    }

    @JvmStatic
    fun trimMemory(level: Int) {
        LruCache.trimMemory(level)
    }

    @JvmStatic
    fun clearMemory() {
        LruCache.clearMemory()
    }

    /**
     * load bitmap by file path, url, or asserts path
     *
     * @param path image path
     */
    @JvmStatic
    fun load(path: String): Request {
        return Request(path)
    }

    /**
     * load bitmap from drawable or raw resource
     *
     * @param resID drawable id or raw id
     */
    @JvmStatic
    fun load(resID: Int): Request {
        return Request(resID)
    }

    @JvmStatic
    fun load(uri: Uri): Request {
        return Request(uri)
    }

    @JvmStatic
    fun downloadOnly(url: String): File? {
        return Downloader.downloadOnly(url)
    }

    @JvmStatic
    fun hasSourceCache(url: String): Boolean {
        return Downloader.hasCache(url)
    }

    /**
     * @param tag         identify the bitmap
     * @param bitmap      bitmap
     * @param toWeakCache cache to [WeakCache] if true,
     * otherwise cache to [LruCache]
     */
    @JvmStatic
    @JvmOverloads
    fun cacheBitmap(tag: String, bitmap: Bitmap, toWeakCache: Boolean = true) {
        MemoryCache.putBitmap(MHash.hash64(tag), bitmap, toWeakCache)
    }

    @JvmStatic
    fun getCacheBitmap(tag: String): Bitmap? {
        return MemoryCache.getBitmap(MHash.hash64(tag))
    }

    /**
     * Stop to put requests to [Worker]
     */
    @JvmStatic
    fun pauseRequest() {
        Dispatcher.pause()
    }

    /**
     * resume requests
     */
    @JvmStatic
    fun resumeRequest() {
        Dispatcher.resume()
    }

    /**
     * notify holder destroy,
     * when all holders of bitmap destroy, the bitmap may be reused.
     *
     * @param host may be Activity, Fragment, or Dialog
     */
    @JvmStatic
    fun notifyEvent(host: Any, event: Int) {
        LifecycleManager.notify(host, event)
    }
}

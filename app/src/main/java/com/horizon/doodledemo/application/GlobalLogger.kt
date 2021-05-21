package com.horizon.doodledemo.application

import android.util.Log
import com.horizon.doodle.worker.ILogger

import com.horizon.doodledemo.BuildConfig


object GlobalLogger : ILogger {
    override val isDebug: Boolean
        get() = BuildConfig.DEBUG

    override fun e(tag: String, e: Throwable) {
        Log.e(tag, e.message, e)
    }
}

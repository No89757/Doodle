package com.horizon.doodle.worker


interface ILogger {
    val isDebug: Boolean

    fun e(tag: String, e: Throwable)
}

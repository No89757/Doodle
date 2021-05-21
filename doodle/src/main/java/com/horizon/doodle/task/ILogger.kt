package com.horizon.doodle.task


interface ILogger {
    val isDebug: Boolean

    fun e(tag: String, e: Throwable)
}

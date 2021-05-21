package com.horizon.doodle.task


object LogProxy : ILogger {
    private var subject: ILogger? = null

    override val isDebug: Boolean
        get() = subject?.isDebug ?: false

    /**
     * init LogProxy when app start
     */
    fun init(realSubject: ILogger) {
        subject = realSubject
    }

    override fun e(tag: String, e: Throwable) {
        subject?.e(tag, e)
    }
}

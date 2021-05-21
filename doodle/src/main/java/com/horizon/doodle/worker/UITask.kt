package com.horizon.doodle.worker

import android.os.Handler
import android.os.Looper
import android.os.Process
import android.text.TextUtils
import android.util.Log
import com.horizon.doodle.worker.lifecycle.LifeEvent
import com.horizon.doodle.worker.lifecycle.LifeListener
import com.horizon.doodle.worker.lifecycle.LifecycleManager
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基于[android.os.AsyncTask], 做了一些扩展。
 * 使用方法和[android.os.AsyncTask]基本相同，部分特性有差异，支持更多的特性。
 *
 * 1. 支持优先级；
 * 2. 随 Activity/Fragment 销毁而自动取消任务（需要设定[host])；
 * 3. 随 Activity/Fragment 切换 可见/不可见 而自动变更优先级；
 * 4. 更多的控制能力：限制并发和避免任务重复执行（具体看[executor]的类型和参数）。
 *
 */
abstract class UITask<Params, Progress, Result> : LifeListener {
    private val mWorker: WorkerRunnable<Params, Result>
    private val mFuture: FutureTask<Result>

    @Volatile
    var isDone = false
        private set
    @Volatile
    var status = Status.PENDING
        private set
    private val mCancelled = AtomicBoolean()
    private val mTaskInvoked = AtomicBoolean()

    private val mFullName: String = this.javaClass.name
    protected val mSimpleName: String

    @Volatile
    private var mPriority = Priority.NORMAL
    private var mHostHash = 0
    private var mExecutionTime: Int = 0

    val isCancelled: Boolean
        get() = mCancelled.get()

    private val mTag: String by lazy { generateTag() }

    /**
     * 通常情况下以全类名为任务标签；
     * 若需要自定义，重写此方法。
     */
    protected open fun generateTag(): String {
        return mFullName
    }

    protected open val executor: PriorityExecutor
        get() = DEFAULT_EXECUTOR

    enum class Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    companion object Companion {
        private val HANDLER = Handler(Looper.getMainLooper())
        val DEFAULT_EXECUTOR = TagExecutor(PipeExecutor(32, 512), true)
    }

    init {
        val simpleName = this.javaClass.simpleName
        mSimpleName = if (TextUtils.isEmpty(simpleName)) {
            val index = mFullName.lastIndexOf('.') + 1
            if (index > 0) mFullName.substring(index) else mFullName
        } else {
            simpleName
        }

        mWorker = object : WorkerRunnable<Params, Result>() {
            @Throws(Exception::class)
            override fun call(): Result? {
                mTaskInvoked.set(true)

                var beginTime: Long = 0
                if (LogProxy.isDebug) {
                    beginTime = System.nanoTime()
                }
                var result: Result? = null
                try {
                    if (!isCancelled) {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        result = doInBackground(*this.mParams)
                    }
                } catch (e: Throwable) {
                    mCancelled.set(true)
                    LogProxy.e(mSimpleName, e)
                    // shall we throw e ?
                }

                if (LogProxy.isDebug) {
                    mExecutionTime = ((System.nanoTime() - beginTime) / 1000000L).toInt()
                }
                postResult(result)
                return result
            }
        }

        mFuture = object : FutureTask<Result>(mWorker) {
            override fun done() {
                this@UITask.isDone = true
                try {
                    postResultIfNotInvoked(get())
                } catch (e: InterruptedException) {
                    Log.w(mSimpleName, e)
                } catch (e: ExecutionException) {
                    throw RuntimeException("An e occured while executing doInBackground()",
                            e.cause)
                } catch (e: CancellationException) {
                    postResultIfNotInvoked(null)
                }
            }

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                this@UITask.mCancelled.set(true)
                return super.cancel(mayInterruptIfRunning)
            }
        }
    }

    internal abstract class WorkerRunnable<Params, Result> : Callable<Result> {
        internal lateinit var mParams: Array<out Params>
    }

    private fun postResultIfNotInvoked(result: Result?) {
        if (mFuture.isCancelled) {
            mCancelled.set(true)
        }
        val wasTaskInvoked = mTaskInvoked.get()
        if (!wasTaskInvoked) {
            postResult(result)
        }
    }

    private fun postResult(result: Result?) {
        HANDLER.post { finish(result) }
    }

    @SafeVarargs
    open fun publishProgress(vararg values: Progress) {
        if (!isCancelled) {
            HANDLER.post { onProgressUpdate(*values) }
        }
    }

    fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return mFuture.cancel(mayInterruptIfRunning)
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    fun get(): Result {
        return mFuture.get()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    operator fun get(timeout: Long, unit: TimeUnit): Result {
        return mFuture.get(timeout, unit)
    }

    fun execute(vararg params: Params) {
        if (status != Status.PENDING) {
            return
        }

        status = Status.RUNNING
        onPreExecute()
        mWorker.mParams = params
        if (executor is TagExecutor) {
            (executor as TagExecutor).execute(mTag, mFuture, mPriority)
        } else {
            (executor as PipeExecutor).execute(mFuture, mPriority)
        }
    }

    private fun finish(result: Result?) {
        detachHost()
        if (isCancelled) {
            logResult("cancel")
            onCancelled(result)
        } else {
            logResult("finish")
            onPostExecute(result)
        }
        status = Status.FINISHED
    }

    private fun logResult(result: String) {
        if (LogProxy.isDebug) {
            val sb = StringBuilder(64)
            sb.append("task ").append(result).append("  ")
            if (mTaskInvoked.get()) {
                sb.append("execute:").append(mExecutionTime).append("ms")
            }
            Log.d(mSimpleName, sb.toString())
        }
    }

    protected open fun onPreExecute() {
    }

    // WorkerThread
    protected abstract fun doInBackground(vararg params: Params): Result?

    protected open fun onPostExecute(result: Result?) {
    }

    protected open fun onProgressUpdate(vararg values: Progress) {
    }

    protected open fun onCancelled(result: Result?) {
        onCancelled()
    }

    protected open fun onCancelled() {
    }

    /**
     * @param priority of task, default is [Priority.NORMAL].
     * @return task itself
     * @see Priority
     */
    fun priority(priority: Int): UITask<Params, Progress, Result> {
        var p = priority
        if (priority != Priority.IMMEDIATE) {
            if (priority > Priority.HIGH) {
                p = Priority.HIGH
            } else if (priority < Priority.LOW) {
                p = Priority.LOW
            }
        }
        mPriority = p
        return this
    }

    private fun changePriority(increment: Int) {
        if (mPriority != Priority.IMMEDIATE) {
            mPriority = executor.changePriority(mFuture, mPriority, increment)
        }
    }

    /**
     * set task's host
     *
     * @param host may be one of Activity, Fragment or Dialog
     * @see LifecycleManager.register
     */
    fun host(host: Any): UITask<Params, Progress, Result> {
        return hostHash(System.identityHashCode(host))
    }

    fun hostHash(hostHash: Int): UITask<Params, Progress, Result> {
        this.mHostHash = hostHash
        LifecycleManager.register(hostHash, this)
        return this
    }

    private fun detachHost() {
        if (mHostHash != 0) {
            LifecycleManager.unregister(mHostHash, this)
        }
    }

    override fun onEvent(event: Int) {
        if (event == LifeEvent.DESTROY) {
            if (!isCancelled && status != Status.FINISHED) {
                // no need to call detachHost for host destroy
                mHostHash = 0
                cancel(true)
            }
        } else if (event == LifeEvent.SHOW) {
            changePriority(+1)
        } else if (event == LifeEvent.HIDE) {
            changePriority(-1)
        }
    }
}
package com.horizon.doodle

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import com.horizon.doodle.interfaces.CacheInterceptor
import com.horizon.doodle.interfaces.Callback
import com.horizon.doodle.interfaces.SimpleTarget
import com.horizon.doodle.transform.Transformation
import com.horizon.task.base.Priority
import java.lang.ref.WeakReference
import java.util.*

/**
 * Request's params has four parts
 */
class Request {
    internal val key: Long by lazy { MHash.hash64(toString()) }

    // image source
    internal var uri: Uri? = null
    internal var path: String
    private var sourceKey: String? = null

    // decode parameter
    internal var viewWidth: Int = 0
    internal var viewHeight: Int = 0
    internal var clipType = Decoder.NO_CLIP
    internal var config = Config.bitmapConfig
    internal var transformations: MutableList<Transformation>? = null
    // decode input original size if set true
    private var noClip = false

    // loading behavior
    internal var gifPriority = true
    internal var priority = Priority.NORMAL
    internal var memoryCacheStrategy = MemoryCacheStrategy.LRU
    internal var onlyIfCached = false
    internal var diskCacheStrategy = DiskCacheStrategy.ALL
    internal var cacheInterceptor: CacheInterceptor? = null
    internal var keepOriginal = false
    internal var placeholderResId = -1
    internal var placeholderDrawable: Drawable? = null
    internal var errorResId = -1
    internal var errorDrawable: Drawable? = null
    internal var goneIfMiss = false
    internal var animationId: Int = 0
    internal var animation: Animation? = null
    internal var crossFade: Boolean = false
    internal var crossFadeDuration: Int = 0
    internal var alwaysAnimation = false
    internal var hostHash: Int = 0

    // target
    internal var waiter: Waiter? = null
    internal var simpleTarget: SimpleTarget? = null
    internal var callback: Callback? = null
    internal var targetReference: WeakReference<ImageView>? = null
    internal var workerReference: WeakReference<Worker>? = null

    /**
     * @param path url or path
     */
    internal constructor(path: String) {
        if (TextUtils.isEmpty(path)) {
            this.path = ""
        } else {
            this.path = if (path.startsWith("http") || path.contains("://")) path else "file://$path"
        }
    }

    /**
     * We use request key to identify bitmap(both memory cache and disk cache), path is part of key.<br></br>
     * Resource name may change input different version (rename or resource confusion). <br></br>
     * Disable disk cache for loading resource image by default. <br></br>
     * You could called [diskCacheStrategy] if you ensured resource name not change. <br></br>
     *
     * @param resID id of drawable or raw
     */
    internal constructor(resID: Int) {
        path = Utils.toUriPath(resID)
        diskCacheStrategy = DiskCacheStrategy.NONE
    }

    internal constructor(uri: Uri?) {
        this.uri = uri
        path = uri?.toString() ?: ""
    }

    /**
     * Sometimes url contains some dynamic parameter, make url vary frequently. <br></br>
     * To make request key stable，you can set sourceKey (remove dynamic parameter),
     * then Doodle will use sourceKey to build request key instead of using path.
     *
     * @param sourceKey the key to identify the image source
     */
    fun sourceKey(sourceKey: String): Request {
        this.sourceKey = sourceKey
        return this
    }

    fun override(width: Int, height: Int): Request {
        this.viewWidth = width
        this.viewHeight = height
        return this
    }

    fun scaleType(scaleType: ImageView.ScaleType): Request {
        this.clipType = Decoder.mapScaleType(scaleType)
        return this
    }

    /**
     * @see [MemoryCacheStrategy]
     */
    fun memoryCacheStrategy(strategy: Int): Request {
        this.memoryCacheStrategy = strategy
        return this
    }

    /**
     * @see DiskCacheStrategy
     */
    fun diskCacheStrategy(strategy: Int): Request {
        this.diskCacheStrategy = strategy
        return this
    }

    /**
     * Not to save and take bitmap from memory or disk.
     *
     *
     * It's no recommended to call this method.
     * But input some case this method might be helpful,
     * For example :
     * 1、Source file may change when path is constant,
     * 2、Debug decoding.
     *
     * @see memoryCacheStrategy
     * @see diskCacheStrategy
     */
    fun noCache(): Request {
        this.memoryCacheStrategy = MemoryCacheStrategy.NONE
        this.diskCacheStrategy = DiskCacheStrategy.NONE
        return this
    }

    /**
     * network control. <br></br>
     * if request source is from network, only try to get data from source cache,
     * otherwise ignore this parameter.
     *
     * @param onlyIfCached only try to get data from source cache if true.
     */
    fun onlyIfCached(onlyIfCached: Boolean): Request {
        this.onlyIfCached = onlyIfCached
        return this
    }

    fun noClip(): Request {
        this.noClip = true
        return this
    }

    fun config(config: Bitmap.Config): Request {
        this.config = config
        return this
    }

    fun transform(transformation: Transformation?): Request {
        if (transformation == null) {
            throw IllegalArgumentException("Transformation must not be null.")
        }
        if (transformations == null) {
            transformations = ArrayList(2)
        }
        transformations!!.add(transformation)
        return this
    }

    /**
     * set priority to loading task
     *
     * @see Priority
     */
    fun priority(priority: Int): Request {
        this.priority = priority
        return this
    }

    /**
     * keep original drawable of target(ImageView) util we get the bitmap of request
     */
    fun keepOriginalDrawable(keep: Boolean): Request {
        keepOriginal = keep
        return this
    }

    fun placeholder(placeholderResId: Int): Request {
        this.placeholderResId = placeholderResId
        return this
    }

    fun placeholder(drawable: Drawable): Request {
        this.placeholderDrawable = drawable
        return this
    }

    fun error(errorResId: Int): Request {
        this.errorResId = errorResId
        return this
    }

    fun error(drawable: Drawable): Request {
        this.errorDrawable = drawable
        return this
    }

    fun goneIfMiss(): Request {
        this.goneIfMiss = true
        return this
    }

    fun animation(animationId: Int): Request {
        this.animationId = animationId
        return this
    }

    fun animation(animation: Animation): Request {
        this.animation = animation
        return this
    }

    @JvmOverloads
    fun fadeIn(duration: Int = 300): Request {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = duration.toLong()
        this.animation = animation
        return this
    }

    @JvmOverloads
    fun crossFade(duration: Int = 300): Request {
        if (duration > 0) {
            this.crossFadeDuration = duration
            crossFade = true
        }
        return this
    }

    fun alwaysAnimation(alwaysAnimation: Boolean): Request {
        this.alwaysAnimation = alwaysAnimation
        return this
    }

    /**
     * Default case, if source file is gif, decode with GifDecoder (if set). <br></br>
     * You can call this method to load bitmap (Not matter source file is gif or other format).
     */
    fun asBitmap(): Request {
        this.gifPriority = false
        return this
    }

    /**
     * set loading task's host
     *
     * If host is Activity and target is ImageView,
     * it's not necessary to call this,
     * cause Doodle will pick the activity automatically by [Utils.pickActivity]
     *
     * @param host may be one of Activity, Fragment or Dialog
     * @see com.horizon.task.UITask.host
     */
    fun host(host: Any?): Request {
        this.hostHash = System.identityHashCode(host)
        return this
    }

    /**
     * You can use CacheInterceptor to cache source file to your own directories.
     */
    fun cacheInterceptor(interceptor: CacheInterceptor): Request {
        this.cacheInterceptor = interceptor
        return this
    }

    /**
     * preload the bitmap.
     *
     * assign sizes with [override], otherwise it will load with original size.
     */
    fun preLoad() {
        fillSizeAndLoad(viewWidth, viewHeight)
    }

    /**
     * get the bitmap on current thread. <br></br>
     * assign sizes with [override], otherwise it will load with original size.
     *
     *
     * It's recommended to call this method input background thread. <br></br>
     * But it's also ok to call this input main thread if you have to do so,
     * input that way, set a short timeout input case of blocking UI or ANR.
     *
     * @param millis timeout for getting bitmap.
     *  * timeout = 0, just try to get the bitmap from memory
     *  * timeout > 0, wait until get the bitmap or out of time
     *  * timeout < 0, throw IllegalArgumentException
     * @return bitmap if hit cache or decode correctly, return null if error occur or out of time.
     * @throws IllegalArgumentException if timeout is negative
     * @throws IllegalStateException    if method not be called from the worker thread
     */
    @JvmOverloads
    operator fun get(millis: Long = 3000L): Bitmap? {
        if (millis < 0) {
            throw IllegalArgumentException("timeout can't be negative")
        }
        gifPriority = false
        this.waiter = Waiter(millis)
        fillSizeAndLoad(viewWidth, viewHeight)
        return waiter?.result
    }

    /**
     * get bitmap by SimpleTarget
     *
     * @see SimpleTarget
     */
    fun into(target: SimpleTarget) {
        this.simpleTarget = target
        fillSizeAndLoad(viewWidth, viewHeight)
    }


    fun into(target: ImageView, callback: Callback) {
        this.callback = callback
        into(target)
    }


    /**
     * Load bitmap into ImageView.<br></br>
     *
     *
     * Don't setTag() to the ImageView, Doodle use tag to identify loading request. <br></br>
     * Try to call [View.setTag] if you want to carry extra data input ImageView
     *
     * @param target ImageView
     */
    fun into(target: ImageView?) {
        if (target == null) {
            return
        }
        targetReference = WeakReference(target)

        if (!noClip && clipType == Decoder.NO_CLIP) {
            clipType = Decoder.mapScaleType(target.scaleType)
        }

        if (noClip) {
            fillSizeAndLoad(0, 0)
        } else if (viewWidth > 0 && viewHeight > 0) {
            fillSizeAndLoad(viewWidth, viewHeight)
        } else if (target.width > 0 && target.height > 0) {
            fillSizeAndLoad(target.width, target.height)
        } else if (Utils.isParamsValid(target.layoutParams)) {
            val params = target.layoutParams
            val pw = params.width
            val ph = params.height
            // if both width and height is wrap_content, load with original size
            if (pw < 0 && ph < 0) {
                fillSizeAndLoad(0, 0)
            } else {
                val w = if (pw > 0) pw else Utils.displayDimens.x
                val h = if (ph > 0) ph else Utils.displayDimens.y
                fillSizeAndLoad(w, h)
            }
        } else if (target.windowToken != null) {
            fillSizeAndLoad(0, 0)
        } else {
            target.viewTreeObserver.addOnPreDrawListener(
                    object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            val view = targetReference!!.get() ?: return true
                            val vto = view.viewTreeObserver
                            if (vto.isAlive) {
                                vto.removeOnPreDrawListener(this)
                            }
                            fillSizeAndLoad(view.width, view.height)
                            return true
                        }
                    })
        }
    }

    private fun fillSizeAndLoad(targetWidth: Int, targetHeight: Int) {
        viewWidth = targetWidth
        viewHeight = targetHeight

        alignParams()

        if (targetReference != null) {
            val target = targetReference!!.get()
            if (target != null && !noClip) {
                val horizonPadding = target.paddingLeft + target.paddingRight
                val verticalPadding = target.paddingTop + target.paddingBottom
                if (viewWidth > horizonPadding && viewHeight > verticalPadding) {
                    viewWidth -= horizonPadding
                    viewHeight -= verticalPadding
                }
            }
        }

        if (hostHash == 0 && targetReference != null) {
            val imageView = targetReference!!.get()
            if (imageView != null) {
                val activity = Utils.pickActivity(imageView)
                hostHash = System.identityHashCode(activity)
            }
        }

        Dispatcher.start(this)
    }

    /**
     * some params are use to build the key,
     * we need to align them to make requests (which map the sample bitmap) input one key.
     */
    private fun alignParams() {
        if (noClip) {
            clipType = Decoder.NO_CLIP
            viewWidth = 0
            viewHeight = 0
        } else if (viewWidth <= 0 || viewHeight <= 0) {
            noClip = true
            clipType = Decoder.NO_CLIP
        }

        // if sizes has been assigned, can not use NO_CLIP mode,
        // use CENTER_INSIDE mode to scale image by assigned width and height
        if (clipType == Decoder.NO_CLIP && viewWidth > 0 && viewHeight > 0) {
            clipType = Decoder.CENTER_INSIDE
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        if (!TextUtils.isEmpty(sourceKey)) {
            builder.append("source:").append(sourceKey)
        } else {
            builder.append("path:").append(path)
        }
        builder.append(" size:").append(viewWidth)
                .append('x').append(viewHeight)
                .append(" type:").append(clipType)
                .append(" config:").append(config)
        if (!Utils.isEmpty(transformations)) {
            builder.append(" transforms:")
            for (transformation in transformations!!) {
                builder.append(' ').append(transformation.key())
            }
        }
        return builder.toString()
    }

    /**
     * Wrapper for getting bitmap synchronously
     */
    internal class Waiter(var timeout: Long) {
        var result: Bitmap? = null
    }
}



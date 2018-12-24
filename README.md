# Doodle
[ ![Download](https://api.bintray.com/packages/horizon757/maven/Doodle/images/download.svg) ](https://bintray.com/horizon757/maven/Doodle/_latestVersion) 

[中文文档](README_CN.md)

Doodle is an efficient image loader for Android.

# Download
```gradle
repositories {
    jcenter()
}

dependencies {
    implementation 'com.horizon.doodle:doodle:1.0.8'
}
```

# How to Use？
## initialize
```kotlin
LogProxy.init(AppLogger)
Doodle.init(context)
        .setDiskCacheCapacity(256L shl 20)
        .setMemoryCacheCapacity(128L shl 20)
        .setDefaultBitmapConfig(Bitmap.Config.ARGB_8888)
        .setGifDecoder(gifDecoder)
```

We hava to notify event to make request cancel when Fragment destroy.
It's not necessary to do this for Activity, for Doodle have done this when calling init(context)

```kotlin
public abstract class BaseFragment extends Fragment {
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Doodle.notifyEvent(this, isVisibleToUser ? LifeEvent.SHOW : LifeEvent.HIDE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Doodle.notifyEvent(this, LifeEvent.DESTROY);
    }
}
```

```kotlin
public class BaseDialog extends Dialog {
    @Override
    public void dismiss() {
        super.dismiss();
        Doodle.notifyEvent(this, LifeEvent.DESTROY);
    }
}
```

## request
```kotlin
Doodle.load(url)
        .host(fragment)
        .placeholder(R.color.colorAccent)
        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
        .into(bottomIv)
```

When make request, if the target is ImageView, Doodle will pick the activity to register host.
But if the request happens in a Fragment, or target is not ImageView,
it's necessary to call host() to make sure the request cancel when **notifyEvent(this, LifeEvent.DESTROY)** calling.

# API 

## Doodle  (Entrance）
method | Effect
---|---
init(Context) : Config | Initialization
trimMemory(int) | trim memory of LruCache
clearMemory() | remove all bitmap from LruCache
load(String): Request | get Request by path
load(int): Request | get Request by resource id
load(Uri): Request | get Request by uri 
downloadOnly(String): File? | download file(no decoding), don't call this method in UI thread
getSourceCacheFile(url: String): File? | get source cache, return null when no cache
cacheBitmap(String,Bitmap,Boolean) |  cache bitmap to memory cache
getCacheBitmap(String): Bitmap? | get bitmap from memory cache
pauseRequest() | pause request
resumeRequest() | resume request
notifyEvent(Any, int) | notify lifecycle event of page(host)

## Config （Global Configuration）
method | effect
---|---
setUserAgent(String) | sey User-Agent header
setDiskCachePath(String) | set disk cache path
setDiskCacheCapacity(Long) |  128M default
setDiskCacheMaxAge(Long) |  30 days default
setSourceCacheCapacity(Long) | 256M default
setMemoryCacheCapacity(Long) | 1/6 of maxMemory() default
setCompressFormat(Bitmap.CompressFormat) | PNG default
setDefaultBitmapConfig(Bitmap.Config) | ARGB_8888 default
setGifDecoder(GifDecoder) | set GifDecoder, Recommend [android-gif-drawable](https://github.com/koral--/android-gif-drawable)

## Request （Single Request）
method | effect
---|---
sourceKey(String) | set key of source, replace path, to build the key of request
override(int, int) | assigned sizes 
scaleType(ImageView.ScaleType) | if target is ImageView, Doodle will pick the ScaleType automatically
| | |
memoryCacheStrategy(int) | LRU default
diskCacheStrategy(int) | ALL default
noCache() | no caching, include memory or disk
onlyIfCached(boolean) | only get source from cache(for http request)
noClip() | no clip or sampling， get full bitmap
config(Bitmap.Config) | ARGB_8888 default 
transform(Transformation) | set transformation
priority(int) | set priority
keepOriginalDrawable() | key original Drawable of ImageView, clear if not calling this
placeholder(int) | placeholder
placeholder(Drawable) | |
error(int) | error drawable
error(Drawable) | |
goneIfMiss() | set imageView.visibility = View.GONE if get result failed.
animation(int) | set animation
animation(Animation) | |
fadeIn(int) | set fade in animation, default duration is 300ms
crossFate(int) | set cross fade animation, default duration is 300ms
alwaysAnimation(Boolean) | only do animation if bitmap is not from memory cache in default case, always do it when calling this.
asBitmap() | not matter source is gif or other format, just decode bitmap 
host(Any) | bind host(page) to request, when page destroy, request cancel. 
cacheInterceptor(CacheInterceptor) | set this to manager cache by yourself
| | |
preLoad() | |
get(get) : Bitmap? | get bitmap in current thread (block util get result or failed)
into(SimpleTarget) | get bitmap by callback (no block)
into(ImageView, Callback) | callback the bitmap in Callback, and return boolean in that method, if return true, Doodle will not setImage again.
into(ImageView?) | load bitmap(or drawable) into ImageView

# License
See the [LICENSE](LICENSE.md) file for license rights and limitations.

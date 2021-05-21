## Doodle

[中文文档](README_CN.md)

Doodle is an efficient image loader (Android platform).

Doodle's API design refers to Picasso and Glide, so if you have used a similar image loader, it's easy to get start.
Compared to Picasso, Doodle's implementation has more detail (cache design, life cycle, task scheduling, GIF support, decoding etc.);
Compared to Glide, Doodle's implementation is more lightweight (400+ methods, 104K aar).


## Download
```gradle
dependencies {
    implementation 'com.horizon.doodle:doodle:1.1.3'
}
```

## How to Use？

### Load Image

Simple case, just set path and target：

```kotlin
Doodle.load(url).into(bottomIv)
```

You can also set more parameters：

```kotlin
Doodle.load(url)
        .placeholder(R.color.colorAccent)
        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
        .into(bottomIv)
```


### Global Config

Doodle has a default configuration in the Config class,
You can define your own configuration by putting parameters to Config.

```kotlin
Doodle.config()
        .setDiskCacheCapacity(256L shl 20)
        .setGifDecoder(gifDecoder)
```



### Life cycle

Doodle implements a lifecycle mechanism.
When the Activity is destroyed, cancel the request refer to the Activity/Fragemtn;
Dynamically change the priority when the Activity/Fragment switch visible/invisible.

If the current page is Activity and the target is ImageView, no more code is required.
Because Doodle has implemented monitoring of Activity lifecycle,
If you haven't set host of Request( by calling host(Any), which is a method of Request), 
Doodle will automatically extract the activity associated with the ImageView as the host.


For observering Fragment's life cycle, you need to call Doodle.notifyEvent() in callback of Fragment, like onDestroy().
Doodle haven't implemented observering life cycle of Fragment,
for considering to inject two lines of code outside is more effective.

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

And set the Fragment to Request:

```kotlin
Doodle.load(url)
		.host(fragment)
		.into(bottomIv)
```


## API 

### Doodle  (Entrance）
method | Effect
---|---
config() : Config | return global config object
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

### Config （Global Configuration）
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

### Request （Single Request）
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
cacheInterceptor(CacheInterceptor) | to cache source file to your own directories.
| | |
preLoad() | |
get(get) : Bitmap? | get bitmap in current thread (block util get result or failed)
into(SimpleTarget) | get bitmap by callback (no block)
into(ImageView, Callback) | callback the bitmap in Callback, and return boolean in that method, if return true, Doodle will not setImage again.
into(ImageView?) | load bitmap(or drawable) into ImageView

## Link
https://www.jianshu.com/p/3df395d8a6bc

## License
See the [LICENSE](LICENSE.md) file for license rights and limitations.

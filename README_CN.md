## Doodle
[ ![Download](https://api.bintray.com/packages/horizon757/maven/Doodle/images/download.svg) ](https://bintray.com/horizon757/maven/Doodle/_latestVersion)

Doodle是一个轻量高效的图片加载框架。<br/>

Doodle的API设计参考了Picasso和Glide，所以用过类似图片加载框架的朋友可以直接上手。<br/>
相比于Picasso，Doodle的实现更加完备（缓存设计，生命周期，任务调度，GIF支持，解码方案等多方面，比Picasso考虑的细节更多）；<br/>
相比于Glide，Doodle的实现更加轻量（方法数400+，包大小104K）。


## 下载
```gradle
dependencies {
    implementation 'com.horizon.doodle:doodle:1.1.3'
}
```

## 如何使用

### 图片加载

最简单的图片加载，只需设定图片路径和ImageView即可。

```kotlin
Doodle.load(url).into(bottomIv)
```

当然也可以设定更多的参数：

```kotlin
Doodle.load(url)
        .placeholder(R.color.colorAccent)
        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
        .into(bottomIv)
```

### 全局配置
Doodle有一套默认的配置(在Config类中)。<br/>
如果需要自己定义配置，可参考以下方法。

```kotlin
Doodle.config()
        .setDiskCacheCapacity(256L shl 20)
        .setGifDecoder(gifDecoder)
```

### 生命周期
Doodle实现了生命周期机制。<br/>
Activity销毁时，取消该页面的图片请求；<br/>
在页面切换 可见/不可见 时动态调整优先级。

如果所在页面是Activity, 并且target是ImageView, 则不需要额外地处理，<br/>
因为Doodle内部实现了对Activity生命周期事件的监听，<br/>
同时,如果没有主动调用Request的host()， Doodle会自动提取ImageView关联的Activity作为host。

如果需要实现对Fragment的监听，需要调用以下代码：

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

考虑到实现对Fragment的事件监听需要额外依赖support包，以及在外部注入两行代码更加高效，<br/>
Doodle没有在框架中实现对Fragment的的事件监听，而是提供方法给外部。


如果需要在Fragment销毁时取消任务，需要在请求时调用host传入Fragment；<br/>
如果没有传Fragment,则Doodle会提取ImageView所在的Activity作为host(如前所述）。

```kotlin
Doodle.load(url)
		.host(fragment)
		.into(bottomIv)
```

## API 
Doodle的API，关注Doodle, Config, Request三个类即可。

### Doodle (框架入口）
方法 | 作用
---|---
config() : Config | 返回全局配置对象
trimMemory(int) | 整理内存(LruCache)，传入ComponentCallbacks2的不同level有不同的策略
clearMemory() | 移除LruCache中所有bitmap
load(String): Request | 传入图片路径，返回Request
load(int): Request | 传入资源ID，返回Request
load(Uri): Request | 传入URI，返回Request
downloadOnly(String): File? | 仅下载图片文件，不解码。此方法会走网络请求，不可再UI线程调用
getSourceCacheFile(url: String): File? | 获取原图缓存，无则返回null。不走网络请求，可以在UI线程调用
cacheBitmap(String,Bitmap,Boolean) |  缓存bitmap到Doodle的MemoryCache, 相当于开放MemoryCache, 复用代码，统一管理。
getCacheBitmap(String): Bitmap? | 获取缓存在Cache中的bitmap
pauseRequest() | 暂停往任务队列中插入请求，对RecycleView快速滑动等场景，可调用此函数
resumeRequest() | 恢复请求
notifyEvent(Any, int) | 发送页面生命周期事件(通知页面销毁以取消请求等）

### Config （全局配置）
方法 | 作用
---|---
setUserAgent(String) | 设置User-Agent头，网络请求将自动填上此Header
setDiskCachePath(String) | 设置结果缓存的存储路径
setDiskCacheCapacity(Long) | 设置结果缓存的容量
setDiskCacheMaxAge(Long) | 设置结果缓存的最大保留时间（从最近一次访问算起），默认30天
setSourceCacheCapacity(Long) | 设置原图缓存的容量
setMemoryCacheCapacity(Long) | 设置内存缓存的容量，默认为maxMemory的1/6
setCompressFormat(Bitmap.CompressFormat) | 设置结果缓存的压缩格式， 默认为PNG
setDefaultBitmapConfig(Bitmap.Config) | 设置默认的Bitmap.Config，默认为ARGB_8888
setGifDecoder(GifDecoder) | 设置GIF解码器, 推荐使用 [android-gif-drawable](https://github.com/koral--/android-gif-drawable)

### Request （图片请求）
方法 | 作用
---|---
sourceKey(String) | 设置数据源的key <br/> url默认情况下作为Request的key的一部分，有时候url有动态的参数，使得url频繁变化，从而无法缓存。此时可以设置sourceKey,提到path作为Request的key的一部分。
override(int, int) | 指定剪裁大小<br/>并不最终bitmap等大小并不一定等于override指定的大小（优先按照 ScaleType剪裁，向下采样），若需确切大小的bitmap可配合ResizeTransformation实现。
scaleType(ImageView.ScaleType) | 指定缩放类型 <br/> 如果target为ImageView则会自动从ImageView获取。
| | |
memoryCacheStrategy(int) | 设置内存缓存策略，默认LRU策略
diskCacheStrategy(int) | 设置磁盘缓存策略，默认ALL
noCache() | 不做任何缓存，包括磁盘缓存和内存缓存
onlyIfCached(boolean) | 指定网络请求是否只从缓存读取（原图缓存）
noClip() | 直接解码，不做剪裁和压缩
config(Bitmap.Config) | 指定单个请求的Bitmap.Config
transform(Transformation) | 设置解码后的图片变换，可以连续调用（会按顺序执行）
priority(int) | 请求优先级
keepOriginalDrawable() | 默认情况下请求开始会先清空ImageView之前的Drawable, 调用此方法后会保留之前的Drawable
placeholder(int) | 设置占位图，在结果加载完成之前会显示此drawable
placeholder(Drawable) | 同上
error(int) | 设置加载失败后的占位图
error(Drawable) | 同上
goneIfMiss() | 加载失败后imageView.visibility = View.GONE
animation(int) | 设置加载成功后的过渡动画
animation(Animation) | 同上
fadeIn(int) | 加载成功后显示淡入动画
crossFate(int) | 这个动画效果是原图从透明度100到0， bitmap从0到100。<br/>当设置placeholder且内存缓存中没有指定图片时， placeholder为原图。<br/>如果没有设置placeholder,  效果和fadeIn差不多。<br/>需要注意的是，这个动画在原图和bitmap宽高不相等时，动画结束时图片会变形。<br/>因此，慎用crossFade。<br/>
alwaysAnimation(Boolean) | 默认情况下仅在图片是从磁盘或者网络加载出来时才做动画，可通过此方法设置总是做动画
asBitmap() | 当设置了GifDecoder时，默认情况下只要图片是GIF图片，则用GifDecoder解码。调用此方法后，只取Gif文件第一帧，返回bitmap
host(Any) | 传入宿主(Activity/Fragment), 以观察其生命周期，参见[Task](https://github.com/No89757/Task)
cacheInterceptor(CacheInterceptor) | 用于缓存原图到指定目录
| | |
preLoad() | 预加载
get(int) : Bitmap? | 当前线程获取图片，加载时阻塞当前线程，可设定timeout时间(默认3s)，超时未完成则取消任务，返回null。
into(SimpleTarget) | 加载图片后通过SimpleTarget回调图片(加载是不阻塞当前线程)
into(ImageView, Callback) | 加载图片图片到ImageView，同时通过Callback回调。如果Callback中返回true, 说明已经处理该bitmap了，则Doodle不会再setBitmap到ImageView了。
into(ImageView?) | 加载图片图片到ImageView

## 相关链接
https://www.jianshu.com/p/3df395d8a6bc

## License
See the [LICENSE](LICENSE.md) file for license rights and limitations.

# Doodle
Doodle is an efficient image loader for Android.

# How to Use？
## initialize
```kotlin
Doodle.init(context)
        .setDiskCacheCapacity(256L shl 20)
        .setMemoryCacheCapacity(128L shl 20)
        .setDefaultBitmapConfig(Bitmap.Config.ARGB_8888)
        .setGifDecoder(gifDecoder)
```

## request
```koltlin
Doodle.load(removedRotateExif)
        .placeholder(R.color.colorAccent)
        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
        .into(bottomIv)
```


# License
See the [LICENSE](LICENSE.md) file for license rights and limitations.
# Doodle
[ ![Download](https://api.bintray.com/packages/horizon757/maven/Doodle/images/download.svg) ](https://bintray.com/horizon757/maven/Doodle/_latestVersion)

Doodle is an efficient image loader for Android.

# Download
```gladle
repositories {
    jcenter()
}

dependencies {
    implementation 'com.horizon.doodle:doodle:1.0.6'
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

## request
```koltlin
Doodle.load(removedRotateExif)
        .placeholder(R.color.colorAccent)
        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
        .into(bottomIv)
```

# License
See the [LICENSE](LICENSE.md) file for license rights and limitations.

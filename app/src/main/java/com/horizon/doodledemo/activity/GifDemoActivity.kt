package com.horizon.doodledemo.activity

import android.os.Bundle
import android.widget.ImageView

import com.horizon.doodle.Doodle
import com.horizon.doodledemo.R
import com.horizon.doodledemo.base.BaseActivity


class GifDemoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif_demo)

        //val url = "https://upload-images.jianshu.io/upload_images/1166341-3f85dd1804020329.gif?imageMogr2/auto-orient/strip%7CimageView2/2/w/700"
        val gifImageView = findViewById<ImageView>(R.id.gif_iv)
        Doodle.load(R.raw.fish).into(gifImageView)
    }
}

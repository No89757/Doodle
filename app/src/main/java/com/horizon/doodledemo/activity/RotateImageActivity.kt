package com.horizon.doodledemo.activity


import android.os.Bundle
import android.widget.ImageView

import com.horizon.doodle.DiskCacheStrategy
import com.horizon.doodle.Doodle
import com.horizon.doodledemo.R
import com.horizon.doodledemo.base.BaseActivity

class RotateImageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rotate_image)

        val topIv = findViewById<ImageView>(R.id.top_iv)
        val bottomIv = findViewById<ImageView>(R.id.bottom_iv)

        val rotateUrl = "http://7xkt2b.com1.z0.glb.clouddn.com/FgXvI-wvFydLlooOOVSYPyCCfWzA"
        val removedRotateExif = "http://7xkt2b.com1.z0.glb.clouddn.com/FgXvI-wvFydLlooOOVSYPyCCfWzA?imageMogr2/strip"
        Doodle.load(rotateUrl)
                .placeholder(R.color.colorAccent)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(topIv)
        Doodle.load(removedRotateExif)
                .placeholder(R.color.colorAccent)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(bottomIv)
    }
}

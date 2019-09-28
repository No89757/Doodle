package com.horizon.doodledemo.activity

import android.os.Bundle
import android.view.View
import com.horizon.doodledemo.R
import com.horizon.doodledemo.base.BaseActivity


class MainActivity : BaseActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.test_image_btn).setOnClickListener(this)
        findViewById<View>(R.id.test_gif_btn).setOnClickListener(this)
        findViewById<View>(R.id.rotate_image).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.test_image_btn -> startActivity(ImageDemoActivity::class.java)
            R.id.test_gif_btn -> startActivity(GifDemoActivity::class.java)
            R.id.rotate_image -> startActivity(RotateImageActivity::class.java)
        }
    }
}

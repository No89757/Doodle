package com.horizon.doodledemo.activity

import android.os.Bundle
import android.widget.ImageView
import com.horizon.doodle.Doodle
import com.horizon.doodledemo.R
import com.horizon.doodledemo.base.BaseActivity


class ImageDemoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_standard_test)

        val testIv = findViewById<ImageView>(R.id.test_iv)

        //val url = "https://pic1.zhimg.com/80/63536f2f01409f750162828a980a0380_hd.jpg"
        //Doodle.load(R.raw.lenna).noCache().into(testIv)

//        Doodle.load(R.raw.lenna)
//                .transform(BlurTransformation())
//                .into(testIv)

        val path = "file:///android_asset/lenna.jpg"
        Doodle.load(path).into(testIv)
    }
}

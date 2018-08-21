package com.horizon.doodledemo.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import com.horizon.doodle.Doodle
import com.horizon.doodledemo.R
import com.horizon.doodledemo.base.BaseActivity
import com.horizon.task.IOTask
import java.io.File


class MainActivity : BaseActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.test_image_btn).setOnClickListener(this)
        findViewById<View>(R.id.test_gif_btn).setOnClickListener(this)
        findViewById<View>(R.id.rotate_image).setOnClickListener(this)

        //new DownloadTask().setHost(this).execute();
    }

    override fun onClick(v: View) {
        val id = v.id
        when (id) {
            R.id.test_image_btn -> startActivity(ImageDemoActivity::class.java)
            R.id.test_gif_btn -> startActivity(GifDemoActivity::class.java)
            R.id.rotate_image -> startActivity(RotateImageActivity::class.java)
            else -> {
            }
        }
    }

    private inner class DownloadTask : IOTask<String, Void, File>() {
        private val url = "https://res.sui.com/img/common/logo-s.png"

        override fun onPreExecute() {
            Log.d(tag, "onPreExecute, hasCache:" + Doodle.hasSourceCache(url))
        }

        override fun doInBackground(vararg strings: String): File? {
            return Doodle.downloadOnly(url)
        }

        override fun onPostExecute(file: File?) {
            Log.d(tag, "onPostExecute, hasCache:" + Doodle.hasSourceCache(url))
            if (file != null) {
                Log.d(tag, "onPostExecute, file:" + file.name + " exists:" + file.exists())
            } else {
                Log.d(tag, "onPostExecute, file is null")
            }
        }
    }
}

package com.horizon.doodledemo.base

import android.content.Intent
import android.support.annotation.UiThread
import android.support.v7.app.AppCompatActivity

@UiThread
abstract class BaseActivity : AppCompatActivity() {
    fun startActivity(activityClazz: Class<*>) {
        val intent = Intent(this, activityClazz)
        startActivity(intent)
    }
}

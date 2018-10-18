package com.horizon.doodledemo.base

import android.content.Intent
import android.support.annotation.UiThread
import android.support.v7.app.AppCompatActivity

@UiThread
abstract class BaseActivity : AppCompatActivity() {
    protected val tag: String = this.javaClass.simpleName

    fun startActivity(activityClazz: Class<*>) {
        val intent = Intent(this, activityClazz)
        startActivity(intent)
    }
}

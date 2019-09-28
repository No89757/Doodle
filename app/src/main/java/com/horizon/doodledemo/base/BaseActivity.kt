package com.horizon.doodledemo.base

import android.content.Intent
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity

@UiThread
abstract class BaseActivity : AppCompatActivity() {
    fun startActivity(activityClazz: Class<*>) {
        val intent = Intent(this, activityClazz)
        startActivity(intent)
    }
}

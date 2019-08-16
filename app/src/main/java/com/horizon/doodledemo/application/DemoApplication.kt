package com.horizon.doodledemo.application

import android.app.Application


class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppInitManager.initApplication()
    }
}

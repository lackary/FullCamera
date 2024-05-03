package com.lacklab.app.fullcamera

import android.app.Application
import android.content.Context
import com.lacklab.app.fullcamera.log.LogcatPlant
import timber.log.Timber

class MainApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Timber.plant(LogcatPlant())
    }
}
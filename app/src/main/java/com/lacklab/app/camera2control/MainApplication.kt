package com.lacklab.app.camera2control

import android.app.Application
import android.content.Context
import com.lacklab.app.camera2control.log.LogcatPlant
import timber.log.Timber

class MainApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Timber.plant(LogcatPlant())
    }
}
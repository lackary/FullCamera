package com.lacklab.app.camera2control

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.lacklab.app.camera2control.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        Timber.d("TEST")
    }

}
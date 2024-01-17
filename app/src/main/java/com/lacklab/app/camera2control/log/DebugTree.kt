package com.lacklab.app.camera2control.log


import androidx.core.os.BuildCompat
import timber.log.Timber

class DebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return  String.format("[%s#%s:%s]",
            super.createStackElementTag(element),
            element.methodName,
            element.lineNumber)
    }
}
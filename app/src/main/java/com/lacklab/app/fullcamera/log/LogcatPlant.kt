package com.lacklab.app.fullcamera.log

import timber.log.Timber

class LogcatPlant : Timber.DebugTree() {
    companion object {
        const val STACK_TRACE_LEVELS_UP = 5
        private val pid = android.os.Process.myPid()
        private val tid = android.os.Process.myTid()
    }

    private lateinit var stackTraceElement: StackTraceElement
    override fun createStackElementTag(element: StackTraceElement): String? {
        return super.createStackElementTag(stackTraceElement)
    }

    override fun v(message: String?, vararg args: Any?) {
        stackTraceElement = getStackTraceElement()
        val message = getLogFormat() + message
        super.v(message, *args)
    }

    override fun d(message: String?, vararg args: Any?) {
        stackTraceElement = getStackTraceElement()
        val message = getLogFormat() + message
        super.d(message, *args)
    }

    override fun i(message: String?, vararg args: Any?) {
        stackTraceElement = getStackTraceElement()
        val message = getLogFormat() + message
        super.i(message, *args)
    }

    override fun w(message: String?, vararg args: Any?) {
        stackTraceElement = getStackTraceElement()
        val message = getLogFormat() + message
        super.e(message, *args)
    }

    override fun e(message: String?, vararg args: Any?) {
        stackTraceElement = getStackTraceElement()
        val message = getLogFormat() + message
        super.e(message, *args)
    }

    private fun getStackTraceElement(): StackTraceElement {
        return StackTraceElement(
            Thread.currentThread().stackTrace[STACK_TRACE_LEVELS_UP+1].className,
            Thread.currentThread().stackTrace[STACK_TRACE_LEVELS_UP+1].methodName,
            Thread.currentThread().stackTrace[STACK_TRACE_LEVELS_UP+1].fileName,
            Thread.currentThread().stackTrace[STACK_TRACE_LEVELS_UP+1].lineNumber)
    }

    private fun getLogFormat(): String {
        return "[${stackTraceElement.methodName}() - ${stackTraceElement.lineNumber}]: "
    }
}
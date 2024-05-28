package com.lacklab.app.fullcamera.util.cam.ability

import android.hardware.camera2.CameraCharacteristics

enum class AwbMode(val mode: Int) {
    OFF(CameraCharacteristics.CONTROL_AWB_MODE_OFF),
    AUTO(CameraCharacteristics.CONTROL_AWB_MODE_AUTO),
    INCANDESCENT(CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT(CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT),
    WARM_FLUORESCENT(CameraCharacteristics.CONTROL_AWB_MODE_WARM_FLUORESCENT),
    DAYLIGHT(CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT),
    TWILIGHT(CameraCharacteristics.CONTROL_AWB_MODE_TWILIGHT),
    SHADE(CameraCharacteristics.CONTROL_AWB_MODE_SHADE);

    companion object {
        fun formInt(value: Int) = entries.firstOrNull { it.mode == value }
    }
}
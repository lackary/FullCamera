package com.lacklab.app.fullcamera.util.cam.ability

import android.hardware.camera2.CameraCharacteristics

enum class AeMode(val mode: Int) {
    OFF(CameraCharacteristics.CONTROL_AE_MODE_OFF),
    ON(CameraCharacteristics.CONTROL_AE_MODE_ON),
    ON_AUTO_FLASH(CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH),
    ON_ALWAYS_FLASH(CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH),
    ON_AUTO_FLASH_REDEYE(CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);

    companion object {
        fun formInt(value: Int) = entries.firstOrNull { it.mode == value}
    }
}
package com.lacklab.app.fullcamera.util.cam.ability

import android.graphics.ImageFormat

enum class CameraFormat(val format: Int) {
    RAW_SENSOR(ImageFormat.RAW_SENSOR),
    JPEG(ImageFormat.JPEG),
    PRIVATE(ImageFormat.PRIVATE),
    YUV_420_888(ImageFormat.YUV_420_888),
    RAW_PRIVATE(ImageFormat.RAW_PRIVATE),
    RAW10(ImageFormat.RAW10),
    DEPTH16(ImageFormat.DEPTH16),
    DEPTH_JPEG(ImageFormat.DEPTH_JPEG),
    Y8(ImageFormat.Y8);

    companion object {
        fun formInt(value: Int) = entries.firstOrNull { it.format == value }
    }
}
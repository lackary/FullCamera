package com.lacklab.app.fullcamera.data

import android.graphics.ImageFormat
import android.os.Parcelable
import android.util.Range
import android.util.Rational
import android.util.Size
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class CameraDevice2Info(
    val logicalCameraId: String,
    val physicalCameraIds: Set<String>?,
    val isMultipleCamera: Boolean,
    var fpsRange: @RawValue Range<Int> = Range(0, 0),
    var previewFormat: Int = ImageFormat.YUV_420_888,
    var format: Int = ImageFormat.JPEG,
    var previewSize: Size = Size(0, 0),
    var size: Size = Size(0,0),
    var exposureCompensationRange: @RawValue Range<Int>? = Range(0,0),
    var exposureCompensationStep: Rational? = Rational(0, 0),
    var exposureTimeRange: @RawValue Range<Int> = Range(10000, 10000),
    var isoRange: @RawValue Range<Int>? = Range(100, 800),
    var zoom: @RawValue Range<Float>? = Range(0f, 0f),
) : Parcelable

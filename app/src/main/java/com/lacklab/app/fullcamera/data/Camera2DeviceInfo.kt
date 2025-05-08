package com.lacklab.app.fullcamera.data

import android.graphics.ImageFormat
import android.os.Parcelable
import android.util.Size
import kotlinx.parcelize.Parcelize

/*
    When we use @Parcelize and these parameters include android.util.Range<T>
    and android.util.Rational.
    DON'T USE @RawValue, Because it will cause crash when app back to background
*/
@Parcelize
data class CameraDevice2Info(
    val logicalCameraId: String,
    val physicalCameraIds: Set<String>? = null,
    val isMultipleCamera: Boolean = false,
    var fpsRange: IntRange? = null,
    var previewFormat: Int = ImageFormat.YUV_420_888,
    var format: Int = ImageFormat.JPEG,
    var previewSize: Size? = null,
    var size: Size? = null,
    var exposureCompensationRange: IntRange? = null,
    var exposureCompensationStep: Float? = null,
    var exposureTimeRange: IntRange? = null,
    var isoRange: IntRange? = null,
    var zoom: FloatRangeData? = null,
) : Parcelable

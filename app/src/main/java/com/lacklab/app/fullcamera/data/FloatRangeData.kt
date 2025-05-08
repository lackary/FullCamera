package com.lacklab.app.fullcamera.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FloatRangeData(
    val start: Float,
    val endInclusive: Float
) : Parcelable {
    companion object {
        fun from(range: ClosedFloatingPointRange<Float>) =
            FloatRangeData(range.start, range.endInclusive)
    }
    fun toRange(): ClosedFloatingPointRange<Float> = start..endInclusive
}

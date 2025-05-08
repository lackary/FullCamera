package com.lacklab.app.fullcamera.util

import android.util.Range
import android.util.Rational

object CovertHelper {
    // Range<Int> ↔ IntRange
    fun Range<Int>.toIntRange() = this.lower..this.upper
    fun IntRange.toRange() = Range(this.first, this.last)

//    // Range<Float> ↔ ClosedFloatingPointRange<Float>
//    fun Range<Float>.toFloatRange() = this.lower..this.upper
//    fun ClosedFloatingPointRange<Float>.toRange() = Range(this.start, this.endInclusive)

//     Rational ↔ Float
//    fun Rational?.toFloatValue(): Float = this?.toFloat() ?: 0f
//    fun Float.toRational(): Rational = Rational((this * 1000000).toInt(), 1000000)
}
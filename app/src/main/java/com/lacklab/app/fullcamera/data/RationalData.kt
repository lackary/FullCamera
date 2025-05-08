package com.lacklab.app.fullcamera.data

import android.os.Parcelable
import android.util.Rational
import kotlinx.parcelize.Parcelize

@Parcelize
data class RationalData(
    val numerator: Int,
    val denominator: Int
) : Parcelable {
    companion object {
        fun from(r: Rational): RationalData = RationalData(r.numerator, r.denominator)
    }
    fun toFloat(): Float = numerator.toFloat() / denominator
    fun toRational(): Rational = Rational(numerator, denominator)
}

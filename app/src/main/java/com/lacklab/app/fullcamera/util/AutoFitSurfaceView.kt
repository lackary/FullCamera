package com.lacklab.app.fullcamera.util

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView
import timber.log.Timber
import kotlin.math.roundToInt

class AutoFitSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle) {

    private var aspectRatio = 0f

    fun setAspectRatio(width: Int, height: Int) {
        require(width > 0 && height > 0) { "Size cannot be negative" }
        Timber.d("width: $width, height:$height")
        aspectRatio = width.toFloat() / height.toFloat()
        holder.setFixedSize(width, height)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        Timber.d("width: $width, height:$height")
        if (aspectRatio == 0f) {
            setMeasuredDimension(width, height)
        } else {
            // WYSIWYG
            val newWidth: Int
            val newHeight: Int
            val actualRatio = if (width > height) aspectRatio else 1f / aspectRatio

            if (width < height * actualRatio) {
                newWidth = width
                newHeight = (width / actualRatio).roundToInt()
            } else {
                newHeight = height
                newWidth = (height * actualRatio).roundToInt()
            }

            Timber.d("Measured dimensions set: $newWidth x $newHeight")
            setMeasuredDimension(newWidth, newHeight)
        }
    }
}
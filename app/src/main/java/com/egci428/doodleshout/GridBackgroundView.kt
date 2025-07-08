package com.egci428.doodleshout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class GridBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
    }

    private val gridSpacing = 50f
    private var x = 0f
    private var y = 0f
    private var velocityX = 2.0f
    private var velocityY = 1.0f

    private val frameRunnable = object : Runnable {
        override fun run() {
            x += velocityX
            y += velocityY
            invalidate()
            postOnAnimation(this)
//            Log.d("DoodleDebug", "Test")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(frameRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frameRunnable)
        super.onDetachedFromWindow()
    }

    // Call every frame
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var X = x % gridSpacing
        while (X <= width) {
            canvas.drawLine(X, 0f, X, height.toFloat(), gridPaint)
            X += gridSpacing
        }

        var Y = y % gridSpacing
        while (Y <= height) {
            canvas.drawLine(0f, Y, width.toFloat(), Y, gridPaint)
            Y += gridSpacing
        }
    }
}

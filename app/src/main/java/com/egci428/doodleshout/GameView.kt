package com.egci428.doodleshout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.scale

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
    }

    private val gridSpacing = 50f
    private var offset = 0f

    private val frameRunnable = object : Runnable {
        override fun run() {
            // Horizontal handled by accelerometer
            // Vertical physics
            if (isJumping) {
                doodlerY += doodlerVelocityY
                doodlerVelocityY += gravity
                // Land on platform
                if (doodlerY + doodlerHeight >= platformY) {
                    doodlerY = platformY - doodlerHeight + 12f
                    doodlerVelocityY = 0f
                    isJumping = false
                }
            }
            invalidate()
            postOnAnimation(this)
        }
    }

    private var doodlerBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.doodler)
    private var doodlerX: Float = 0f
    private var doodlerY: Float = 0f
    private var doodlerWidth: Int = 0
    private var doodlerHeight: Int = 0

    private val platformPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private var platformWidth: Float = 0f
    private var platformHeight: Float = 0f
    private var platformX: Float = 0f
    private var platformY: Float = 0f

    private var doodlerVelocityX: Float = 0f
    private val doodlerSpeed = 5f // Reduced for slower movement

    private var doodlerVelocityY: Float = 0f
    private var gravity: Float = 1.2f
    private var isJumping: Boolean = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(frameRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frameRunnable)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        platformWidth = width.toFloat()
        platformHeight = 24f
        platformX = 0f
        platformY = height - platformHeight - 16f

        doodlerWidth = width / 6
        doodlerHeight = (doodlerBitmap.height * (doodlerWidth.toFloat() / doodlerBitmap.width)).toInt()
        doodlerBitmap = doodlerBitmap.scale(doodlerWidth, doodlerHeight)
        doodlerX = (width - doodlerWidth) / 2f
        doodlerY = platformY - doodlerHeight + 12f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        canvas.drawRect(platformX, platformY, platformX + platformWidth, platformY + platformHeight, platformPaint)
        canvas.drawBitmap(doodlerBitmap, doodlerX, doodlerY, null)
    }

    private fun drawBackground(canvas: Canvas) {
        var x = 0.0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSpacing
        }

        var y = -offset % gridSpacing
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSpacing
        }
    }

    fun onAccelerometerChanged(x: Float) {
        doodlerVelocityX = -x * doodlerSpeed
        doodlerX += doodlerVelocityX
        if (doodlerX + doodlerWidth < 0) {
            doodlerX = width.toFloat()
        }
        else if (doodlerX > width) {
            doodlerX = -doodlerWidth.toFloat()
        }
        invalidate()
    }

    fun jump(strength: Float) {
        if (!isJumping && doodlerY + doodlerHeight >= platformY) {
            doodlerVelocityY = -28f * strength // Negative is up
            isJumping = true
        }
    }
}
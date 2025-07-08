package com.egci428.doodleshout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

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
    private val platformChance = 70 // percent
    private val platformPerScreen = 10

    // Game over state
    private var isGameOver = false
    private var gameOverCallback: (() -> Unit)? = null
    private var restartGameCallback: (() -> Unit)? = null


    private val frameRunnable = object : Runnable {
        @SuppressLint("DefaultLocale")
        override fun run() {
            if (isGameOver) return

            // Horizontal handled by accelerometer
            // Vertical physics
            doodlerY += doodlerVelocityY

            doodlerVelocityY += gravity
            var nothing = true
            var maxPlatformY = 0.0f
            for (platform in platformList) {
                if (platform.y > score + height + 50) {
                    platformList.remove(platform)
                    break
                }
                if (platform.checkCollision(doodlerX, doodlerY, doodlerWidth, doodlerHeight, doodlerVelocityX, doodlerVelocityY) && doodlerVelocityY >= 0) {
                    jump(1.5f)
                    nothing = false
                    break
                }
                maxPlatformY = max(maxPlatformY, platform.y)
            }

            // Land on platform
            if (doodlerY + doodlerHeight > platformY) {
                doodlerY = platformY - doodlerHeight
                doodlerVelocityY = 0f
            }

            if (doodlerY < score + height / 3) {
                score = min(score, doodlerY.toInt() - height / 3)
            }

            if (doodlerY < lastPlatformY + 3000) {
                generatePlatform()
            }

            // Check for game over - if doodler falls below the screen
            if (doodlerY - score > height + 200) {
                triggerGameOver()
            }

            invalidate()
            postOnAnimation(this)
        }
    }

    private var score = 0

    private var random = Random(System.currentTimeMillis())
    private var doodlerBitmapRight: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.doodler)
    private var doodlerBitmapLeft: Bitmap = doodlerBitmapRight.flipHorizontally()
    private var currentDoodlerBitmap = doodlerBitmapRight
    private var doodlerX: Float = 0f
    private var doodlerY: Float = 0f
    private var doodlerWidth: Int = 0
    private var doodlerHeight: Int = 0
    private var lastPlatformY: Float = 0f

    private var platformList = ArrayList<PlatformEntity>()
    private var platformBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.platform)
    private val platformPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val debugPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
        textSize = 100.0f
    }
    private val fallingPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
    }
    private val scorePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 5.0f
        textSize = 100.0f
        typeface = resources.getFont(R.font.doodlejump)
    }
    private val gameOverPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 3.0f
        textSize = 120.0f
        typeface = resources.getFont(R.font.doodlejump)
        textAlign = Paint.Align.CENTER
    }
    private val gameOverSubPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2.0f
        textSize = 60.0f
        typeface = resources.getFont(R.font.doodlejump)
        textAlign = Paint.Align.CENTER
    }
    private val gameOverBackgroundPaint = Paint().apply {
        color = Color.argb(200, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private var platformWidth: Float = 0f
    private var platformHeight: Float = 0f
    private var platformX: Float = 0f
    private var platformY: Float = 0f

    private var doodlerVelocityX: Float = 0f
    private val doodlerSpeed = 0.5f // Reduced for slower movement

    private var doodlerVelocityY: Float = 0f
    private var gravity: Float = 0.7f
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
        platformWidth = width / 12.0f
        platformHeight = (platformBitmap.height * (platformWidth.toFloat() / platformBitmap.width))
        platformX = 0f
        platformY = height - 16f - platformHeight
        platformBitmap = platformBitmap.scale(platformWidth.toInt(), platformHeight.toInt())

        doodlerWidth = width / 8
        doodlerHeight = (currentDoodlerBitmap.height * (doodlerWidth.toFloat() / currentDoodlerBitmap.width)).toInt()
        debug("width: ${doodlerWidth}, height: ${doodlerHeight}")
        doodlerBitmapRight = doodlerBitmapRight.scale(doodlerWidth, doodlerHeight)
        doodlerBitmapLeft = doodlerBitmapLeft.scale(doodlerWidth, doodlerHeight)

        doodlerX = (width - doodlerWidth) / 2f
        doodlerY = platformY - doodlerHeight - 100
        debug("x: ${doodlerX}, y: ${doodlerY}")

        //

        isJumping = false
        jump(2.0f)

        lastPlatformY = height * 1.0f

        for (i in 0 .. 80) {
            generatePlatform()
        }

    }

    private fun generatePlatform() {
        lastPlatformY -= height / platformPerScreen
        if (random.nextInt(100) >= platformChance) {
            return
        }
        var x = random.nextInt(0, width - platformBitmap.width).toFloat()
        platformList.add(PlatformEntity(x, lastPlatformY, platformBitmap.width, platformBitmap.height))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        currentDoodlerBitmap = if (doodlerVelocityX > 0) doodlerBitmapRight else doodlerBitmapLeft
//        canvas.drawRect(doodlerX, doodlerY - score, doodlerX + doodlerWidth, doodlerY + doodlerHeight - score, if (doodlerVelocityY >= 0) fallingPaint else debugPaint)
        for(platform in platformList) {
            canvas.drawBitmap(platformBitmap, platform.x, platform.y - score, null)
//            canvas.drawLine(platform.x, platform.y - score, platform.x + platform.width, platform.y - score, debugPaint)
//            canvas.drawText(platformList.indexOf(platform).toString(), platform.x, platform.y - score, debugPaint)
        }
        canvas.drawBitmap(currentDoodlerBitmap, doodlerX, doodlerY - score, null)
        canvas.drawText("Score: ${score / -150}", 20.0f, 120.0f, scorePaint)

        // Draw game over overlay
        if (isGameOver) {
            drawGameOver(canvas)
        }
    }

    private fun drawGameOver(canvas: Canvas) {
        // Draw semi-transparent background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gameOverBackgroundPaint)

        // Draw game over text
        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawText("GAME OVER", centerX, centerY - 100, gameOverPaint)
        canvas.drawText("Final Score: ${score / -150}", centerX, centerY - 20, gameOverSubPaint)
        canvas.drawText("Tap to restart", centerX, centerY + 60, gameOverSubPaint)
    }

    private fun drawBackground(canvas: Canvas) {
        var x = 0.0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSpacing
        }

        var y = -score % gridSpacing
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSpacing
        }
    }

    fun onAccelerometerChanged(x: Float) {
        if (isGameOver) return

        doodlerVelocityX += -x * doodlerSpeed
        doodlerVelocityX = min(doodlerVelocityX, 200.0f)
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
        if (isGameOver) return
        doodlerVelocityY = -28f * strength // Negative is up
    }

    fun boost(strength: Float) {
        if (isGameOver) return
        debug("Boost: ${strength}")
        doodlerVelocityY -= 1.5f * strength
        doodlerVelocityY = min(doodlerVelocityY, 100.0f)
    }

    private fun triggerGameOver() {
        isGameOver = true
        debug("Game Over! Final Score: ${score / -150}")
        gameOverCallback?.invoke()
        MainActivity.SQLiteHelper.insertScore(score / -150)
        MainActivity.updateLeaderboard()
    }

    fun setGameOverCallback(callback: () -> Unit) {
        gameOverCallback = callback
    }

    fun setRestartGameCallback(callback: () -> Unit) {
        restartGameCallback = callback
    }

    fun restartGame() {
        isGameOver = false
        restartGameCallback?.invoke()
        score = 0
        doodlerX = (width - doodlerWidth) / 2f
        doodlerY = platformY - doodlerHeight - 100
        doodlerVelocityX = 0f
        doodlerVelocityY = 0f
        lastPlatformY = height * 1.0f
        platformList.clear()

        for (i in 0 .. 80) {
            generatePlatform()
        }

        jump(2.0f)
        postOnAnimation(frameRunnable)
    }

    fun isGameOver(): Boolean {
        return isGameOver
    }

    // To flip horizontally:
    fun Bitmap.flipHorizontally(): Bitmap {
        val matrix = Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    // To flip vertically:
    fun Bitmap.flipVertically(): Bitmap {
        val matrix = Matrix().apply { postScale(1f, -1f, width / 2f, height / 2f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    fun debug(str: String) {
        Log.d("DoodleDebug", str)
    }
}
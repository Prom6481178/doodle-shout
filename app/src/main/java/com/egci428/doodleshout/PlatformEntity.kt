package com.egci428.doodleshout

import android.util.Log

// Create a platform entity to set the platform position (x,y) and size (width, height)
class PlatformEntity(var x: Float, var y: Float, var width: Int, var height: Int) {

    init {
        Log.d("DoodleDebug", "${x}, ${y}")
    }

    // Doodle hitbox to check if doodle hit the platform
    fun checkCollision(x1: Float, y1: Float, w1: Int, h1: Int, vx: Float, vy: Float): Boolean {
        // Check X
        return x1 <= x + width && x <= x1 + w1 &&
                // Check Y
                y1 + h1 >= y && y + height >= y1 + h1 ||
                // Check X
                x1 <= x + width && x <= x1 + w1 &&
                // Next tick Y
                y1 + h1 <= y && y <= y1 + h1 + vy
    }
}
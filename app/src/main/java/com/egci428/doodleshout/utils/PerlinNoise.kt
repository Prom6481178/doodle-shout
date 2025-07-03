package com.egci428.doodleshout.utils

import kotlin.math.*

class PerlinNoise(private val seed: Int = 0) {
    private val permutation = IntArray(512)

    init {
        val p = IntArray(256) { it }
        p.shuffle()
        for (i in 0 until 512) {
            permutation[i] = p[i % 256]
        }
    }

    private fun fade(t: Double) = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(t: Double, a: Double, b: Double) = a + t * (b - a)
    private fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return ((if (h and 1 == 0) u else -u) + (if (h and 2 == 0) v else -v))
    }

    fun noise(x: Double, y: Double = 0.0, z: Double = 0.0): Double {
        val X = floor(x).toInt() and 255
        val Y = floor(y).toInt() and 255
        val Z = floor(z).toInt() and 255

        val xf = x - floor(x)
        val yf = y - floor(y)
        val zf = z - floor(z)

        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        val A = permutation[X] + Y
        val AA = permutation[A] + Z
        val AB = permutation[A + 1] + Z
        val B = permutation[X + 1] + Y
        val BA = permutation[B] + Z
        val BB = permutation[B + 1] + Z

        return lerp(w, lerp(v,
            lerp(u, grad(permutation[AA], xf, yf, zf),
                grad(permutation[BA], xf - 1, yf, zf)),
            lerp(u, grad(permutation[AB], xf, yf - 1, zf),
                grad(permutation[BB], xf - 1, yf - 1, zf))),
            lerp(v,
                lerp(u, grad(permutation[AA + 1], xf, yf, zf - 1),
                    grad(permutation[BA + 1], xf - 1, yf, zf - 1)),
                lerp(u, grad(permutation[AB + 1], xf, yf - 1, zf - 1),
                    grad(permutation[BB + 1], xf - 1, yf - 1, zf - 1))))
    }
}
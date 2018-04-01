package com.soywiz.korau.format.util

import com.soywiz.kmem.countLeadingZeros
import com.soywiz.korio.math.rint
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.write16_le
import kotlin.math.*

object CodecUtils {
    // FLT_EPSILON the minimum positive number such that 1.0 + FLT_EPSILON != 1.0
    val FLT_EPSILON = 1.19209290E-07f
    val M_SQRT1_2 = 0.707106781186547524401f // 1/sqrt(2)
    val M_PI = PI.toFloat()
    val M_SQRT2 = 1.41421356237309504880f // sqrt(2)

    val ff_log2_tab = intArrayOf(0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7)

    private val log2 = ln(2.0).toFloat()

    private fun convertSampleFloatToInt16(sample: Float): Int =
            min(max((sample * 32768f + 0.5f).toInt(), -32768), 32767) and 0xFFFF

    fun writeOutput(samples: Array<FloatArray>, output: SyncStream, numberOfSamples: Int, decodedChannels: Int, outputChannels: Int) {
        when (outputChannels) {
            1 -> for (i in 0 until numberOfSamples) {
                val sample = convertSampleFloatToInt16(samples[0][i])
                output.write16_le(sample)
            }
            2 -> if (decodedChannels == 1) {
                // Convert decoded mono into output stereo
                for (i in 0 until numberOfSamples) {
                    val sample = convertSampleFloatToInt16(samples[0][i])
                    output.write16_le(sample)
                    output.write16_le(sample)
                }
            } else {
                for (i in 0 until numberOfSamples) {
                    val lsample = convertSampleFloatToInt16(samples[0][i])
                    val rsample = convertSampleFloatToInt16(samples[1][i])
                    output.write16_le(lsample)
                    output.write16_le(rsample)
                }
            }
        }
    }

    fun avLog2(n: Int): Int = if (n == 0) 0 else 31 - n.countLeadingZeros()
    fun log2f(n: Float): Float = ln(n.toDouble()).toFloat() / log2
    fun lrintf(n: Float): Int = rint(n.toDouble()).toInt()
    fun exp2f(n: Float): Float = 2.0.pow(n.toDouble()).toFloat()
    fun sqrtf(n: Float): Float = sqrt(n.toDouble()).toFloat()
    fun cosf(n: Float): Float = cos(n.toDouble()).toFloat()
    fun sinf(n: Float): Float = sin(n.toDouble()).toFloat()
    fun atanf(n: Float): Float = atan(n.toDouble()).toFloat()
    fun atan2f(y: Float, x: Float): Float = atan2(y.toDouble(), x.toDouble()).toFloat()
}


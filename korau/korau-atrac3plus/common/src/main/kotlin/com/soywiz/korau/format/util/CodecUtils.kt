package com.soywiz.korau.format.util

import com.soywiz.kmem.*
import com.soywiz.korio.stream.*
import kotlin.math.*

object CodecUtils {
	private fun convertSampleFloatToInt16(sample: Float): Int =
		min(max((sample * 32768f + 0.5f).toInt(), -32768), 32767) and 0xFFFF

	fun writeOutput(
		samples: Array<FloatArray>,
		output: SyncStream,
		numberOfSamples: Int,
		decodedChannels: Int,
		outputChannels: Int
	) {
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
}


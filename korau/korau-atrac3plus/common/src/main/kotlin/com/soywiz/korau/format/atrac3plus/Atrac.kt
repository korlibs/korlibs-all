package com.soywiz.korau.format.atrac3plus

import com.soywiz.kmem.arraycopy
import kotlin.math.pow

/*
 * Based on the FFmpeg version from Maxim Poliakovski.
 * All credits go to him.
 * C to Java conversion by gid15 for the jpcsp project.
 * Java to Kotlin for kpspemu
 */
class Atrac {
	private val gainTab1 = FloatArray(16) ///< gain compensation level table
	private val gainTab2 = FloatArray(31) ///< gain compensation interpolation table
	private var id2expOffset: Int = 0               ///< offset for converting level index into level exponent
	private var locScale: Int = 0                   ///< scale of location code = 2^loc_scale samples
	private var locSize: Int = 0                    ///< size of location code in samples

	fun initGainCompensation(id2expOffset: Int, locScale: Int) {
		this.locScale = locScale
		this.locSize = 1 shl locScale
		this.id2expOffset = id2expOffset

		// Generate gain level table
		for (i in 0..15) {
			gainTab1[i] = 2.0.pow((id2expOffset - i).toDouble()).toFloat()
		}

		// Generate gain interpolation table
		for (i in -15..15) {
			gainTab2[i + 15] = 2.0.pow(-1.0 / locSize * i).toFloat()
		}
	}

	fun gainCompensation(`in`: FloatArray, inOffset: Int, prev: FloatArray, prevOffset: Int, gcNow: AtracGainInfo, gcNext: AtracGainInfo, numSamples: Int, out: FloatArray, outOffset: Int) {
		val gcScale = if (gcNext.numPoints != 0) gainTab1[gcNext.levCode[0]] else 1f

		if (gcNow.numPoints == 0) {
			for (pos in 0 until numSamples) {
				out[outOffset + pos] = `in`[inOffset + pos] * gcScale + prev[prevOffset + pos]
			}
		} else {
			var pos = 0

			for (i in 0 until gcNow.numPoints) {
				val lastpos = gcNow.locCode[i] shl locScale

				var lev = gainTab1[gcNow.levCode[i]]
				val gainInc = gainTab2[(if (i + 1 < gcNow.numPoints) gcNow.levCode[i + 1] else id2expOffset) - gcNow.levCode[i] + 15]

				// apply constant gain level and overlap
				while (pos < lastpos) {
					out[outOffset + pos] = (`in`[inOffset + pos] * gcScale + prev[prevOffset + pos]) * lev
					pos++
				}

				// interpolate between two different gain levels
				while (pos < lastpos + locSize) {
					out[outOffset + pos] = (`in`[inOffset + pos] * gcScale + prev[prevOffset + pos]) * lev
					lev *= gainInc
					pos++
				}
			}

			while (pos < numSamples) {
				out[outOffset + pos] = `in`[inOffset + pos] * gcScale + prev[prevOffset + pos]
				pos++
			}
		}

		// copy the overlapping part into the delay buffer
		arraycopy(`in`, inOffset + numSamples, prev, prevOffset, numSamples)
	}

	companion object {
		val ff_atrac_sf_table = FloatArray(64)
		private val qmf_window = FloatArray(48)
		private val qmf_48tap_half = floatArrayOf(-0.00001461907f, -0.00009205479f, -0.000056157569f, 0.00030117269f, 0.0002422519f, -0.00085293897f, -0.0005205574f, 0.0020340169f, 0.00078333891f, -0.0042153862f, -0.00075614988f, 0.0078402944f, -0.000061169922f, -0.01344162f, 0.0024626821f, 0.021736089f, -0.007801671f, -0.034090221f, 0.01880949f, 0.054326009f, -0.043596379f, -0.099384367f, 0.13207909f, 0.46424159f)

		fun generateTables() {
			// Generate scale factors
			if (ff_atrac_sf_table[63] == 0f) {
				for (i in 0..63) {
					ff_atrac_sf_table[i] = 2.0.pow((i - 15) / 3.0).toFloat()
				}
			}

			// Generate the QMF window
			if (qmf_window[47] == 0f) {
				for (i in 0..23) {
					val s = qmf_48tap_half[i] * 2.0f
					qmf_window[i] = s
					qmf_window[47 - i] = s
				}
			}
		}

		fun iqmf(inlo: FloatArray, inloOffset: Int, inhi: FloatArray, inhiOffset: Int, nIn: Int, out: FloatArray, outOffset: Int, delayBuf: FloatArray, temp: FloatArray) {
			var outOffset = outOffset
			arraycopy(delayBuf, 0, temp, 0, 46)

			// loop1
			run {
				var i = 0
				while (i < nIn) {
					temp[46 + 2 * i + 0] = inlo[inloOffset + i] + inhi[inhiOffset + i]
					temp[46 + 2 * i + 1] = inlo[inloOffset + i] - inhi[inhiOffset + i]
					temp[46 + 2 * i + 2] = inlo[inloOffset + i + 1] + inhi[inhiOffset + i + 1]
					temp[46 + 2 * i + 3] = inlo[inloOffset + i + 1] - inhi[inhiOffset + i + 1]
					i += 2
				}
			}

			// loop2
			var p1 = 0
			for (j in nIn downTo 1) {
				var s1 = 0f
				var s2 = 0f

				var i = 0
				while (i < 48) {
					s1 += temp[p1 + i] * qmf_window[i]
					s2 += temp[p1 + i + 1] * qmf_window[i + 1]
					i += 2
				}

				out[outOffset + 0] = s2
				out[outOffset + 1] = s1

				p1 += 2
				outOffset += 2
			}

			// Update the delay buffer.
			arraycopy(temp, nIn * 2, delayBuf, 0, 46)
		}
	}
}

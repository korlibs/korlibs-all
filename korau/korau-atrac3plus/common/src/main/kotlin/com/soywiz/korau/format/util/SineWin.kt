package com.soywiz.korau.format.util

import kotlin.math.PI
import kotlin.math.sin

object SineWin {
	val ff_sine_64 = FloatArray(64)
	val ff_sine_128 = FloatArray(128)
	val ff_sine_512 = FloatArray(512)
	val ff_sine_1024 = FloatArray(1024)

	private fun sineWindowInit(window: FloatArray) {
		val n = window.size
		for (i in 0 until n) {
			window[i] = sin((i + 0.5) * (PI / (2.0 * n))).toFloat()
		}
	}

	fun initFfSineWindows() {
		sineWindowInit(ff_sine_64)
		sineWindowInit(ff_sine_128)
		sineWindowInit(ff_sine_512)
		sineWindowInit(ff_sine_1024)
	}
}

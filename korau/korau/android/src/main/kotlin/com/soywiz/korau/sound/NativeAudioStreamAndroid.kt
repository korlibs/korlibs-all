package com.soywiz.korau.sound

import com.soywiz.korio.async.sleep
import com.soywiz.korio.coroutine.getCoroutineContext

actual class NativeAudioStream actual constructor(val freq: Int = 44100) {
	val msElapsed: Double = 0.0
	val availableBuffers: Int = 0

	actual suspend fun addSamples(samples: ShortArray, offset: Int, size: Int) {
		getCoroutineContext().sleep(1)
		// @TODO
	}
}
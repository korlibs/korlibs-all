package com.soywiz.korau.sound

import com.soywiz.korio.async.sleep
import com.soywiz.korio.coroutine.getCoroutineContext

actual class NativeAudioStream actual constructor(val freq: Int) {
	val msElapsed: Double = 0.0
	val availableBuffers: Int = 0

	actual suspend fun addSamples(samples: ShortArray, offset: Int, size: Int) {
		getCoroutineContext().sleep(1)
		// @TODO
	}

	actual fun stop() {

	}

	actual fun start() {

	}

	actual val availableSamples: Int = 1024
}
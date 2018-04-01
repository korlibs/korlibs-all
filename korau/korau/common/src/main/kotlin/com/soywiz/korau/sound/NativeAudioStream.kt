package com.soywiz.korau.sound

expect class NativeAudioStream(freq: Int = 44100) {
	suspend fun addSamples(samples: ShortArray, offset: Int, size: Int)
}

suspend fun NativeAudioStream.addSamples(samples: ShortArray): Unit = addSamples(samples, 0, samples.size)
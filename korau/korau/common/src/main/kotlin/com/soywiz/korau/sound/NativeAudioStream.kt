package com.soywiz.korau.sound

expect class NativeAudioStream(freq: Int) {
	val availableSamples: Int
	suspend fun addSamples(samples: ShortArray, offset: Int, size: Int)
	fun start()
	fun stop()
}

fun NativeAudioStream(): NativeAudioStream = NativeAudioStream(44100)

suspend fun NativeAudioStream.addSamples(samples: ShortArray): Unit = addSamples(samples, 0, samples.size)

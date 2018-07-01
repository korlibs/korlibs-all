package com.soywiz.korau.sound

expect class NativeAudioStream(freq: Int) {
	val availableSamples: Int
	suspend fun addSamples(samples: ShortArray, offset: Int, size: Int)
	fun start()
	fun stop()
}

// @TODO: kotlin-js BUG: https://youtrack.jetbrains.com/issue/KT-25210
//fun NativeAudioStream(): NativeAudioStream = NativeAudioStream(44100)

fun NewNativeAudioStream(): NativeAudioStream = NativeAudioStream(44100)

suspend fun NativeAudioStream.addSamples(samples: ShortArray): Unit = addSamples(samples, 0, samples.size)

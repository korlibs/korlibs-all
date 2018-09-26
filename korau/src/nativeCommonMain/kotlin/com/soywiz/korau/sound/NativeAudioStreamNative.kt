package com.soywiz.korau.sound

import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlin.coroutines.*
import kotlinx.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider by lazy { DummyNativeSoundProvider() }

actual class NativeAudioStream actual constructor(val freq: Int) {
	actual fun start(): Unit {

	}
	actual fun stop(): Unit {

	}
	actual val availableSamples: Int get() = TODO()

	actual suspend fun addSamples(samples: ShortArray, offset: Int, size: Int): Unit {
		println("NativeAudioStream.addSamples: $offset,$size")
		delay(1)
	}
}

package com.soywiz.korau.sound

import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korio.lang.*
import kotlin.coroutines.experimental.*

actual val nativeSoundProvider: NativeSoundProvider by lazy { TODO() }

actual class NativeAudioStream actual constructor(val freq: Int) {
	actual fun start(): Unit = TODO()
	actual fun stop(): Unit = TODO()
	actual val availableSamples: Int get() = TODO()
	actual suspend fun addSamples(samples: ShortArray, offset: Int, size: Int): Unit = TODO()
}

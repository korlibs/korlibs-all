package com.soywiz.korau.sound

import com.soywiz.korau.format.*
import com.soywiz.korio.async.*
import kotlin.coroutines.experimental.*

class SoundChannel {
	private var channel: NativeSoundChannel? = null
	private var promise: Promise<Unit>? = null

	var volume = 1.0 // @TODO: Handl
		set(value) {
			field = value
			channel?.volume = value
		}


	fun play(sound: NativeSound?) {
		stop()
		channel = sound?.play()
	}

	fun play(stream: AudioStream?, bufferSeconds: Double = 0.1) {
		stop()
		promise = launch(EmptyCoroutineContext) {
			stream?.play(bufferSeconds)
			Unit
		}
	}

	fun stop() {
		channel?.stop()
		promise?.cancel()
		channel = null
		promise = null
	}
}

package com.soywiz.korau.format

import com.soywiz.kmem.arraycopy
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.vfs.VfsFile
import kotlin.math.min

class AudioData(
	val rate: Int,
	val channels: Int,
	val samples: ShortArray
) {
	val seconds: Double get() = (samples.size / channels).toDouble() / rate.toDouble()

	fun convertTo(rate: Int = 44100, channels: Int = 2): AudioData {
		TODO()
	}

	fun toStream() = object : AudioStream(rate, channels) {
		var cursor = 0
		suspend override fun read(out: ShortArray, offset: Int, length: Int): Int {
			val available = samples.size - cursor
			val toread = min(available, length)
			if (toread > 0) arraycopy(samples, cursor, out, offset, toread)
			return toread
		}
	}

	override fun toString(): String = "AudioData(rate=$rate, channels=$channels, samples=${samples.size})"
}

suspend fun AudioData.toNativeSound() = nativeSoundProvider.createSound(this)

suspend fun AudioData.play() = this.toNativeSound().play()

suspend fun VfsFile.readAudioData(formats: AudioFormats = defaultAudioFormats) = this.openUse2 { formats.decode(this) ?: invalidOp("Can't decode audio file ${this@readAudioData}") }
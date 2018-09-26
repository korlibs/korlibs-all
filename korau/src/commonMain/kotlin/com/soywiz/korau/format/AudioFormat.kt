@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korau.format

import com.soywiz.kds.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.file.*

open class AudioFormat(vararg exts: String) {
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()

	data class Info(
		var lengthInMicroseconds: Long = 0L,
		var channels: Int = 2
	) : Extra by Extra.Mixin() {
		val msLength = lengthInMicroseconds / 1000L
		val length = lengthInMicroseconds.toDouble() / 1_000_000.0
	}

	open suspend fun tryReadInfo(data: AsyncStream): Info? = null
	open suspend fun decodeStream(data: AsyncStream): AudioStream? = null
	suspend fun decode(data: AsyncStream): AudioData? = decodeStream(data)?.toData()
	open suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String): Unit = TODO()

	suspend fun encodeToByteArray(
		data: AudioData,
		filename: String = "out.wav",
		format: AudioFormat = this
	): ByteArray {
		val out = MemorySyncStream()
		format.encode(data, out.toAsync(), filename)
		return out.toByteArray()
	}
}

val defaultAudioFormats = AudioFormats().apply { registerStandard() }

class AudioFormats : AudioFormat() {
	val formats = linkedSetOf<AudioFormat>()

	fun register(vararg formats: AudioFormat): AudioFormats = this.apply { this.formats += formats }

	override suspend fun tryReadInfo(data: AsyncStream): Info? {
		for (format in formats) {
			try {
				return format.tryReadInfo(data.duplicate()) ?: continue
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}
		return null
	}

	override suspend fun decodeStream(data: AsyncStream): AudioStream? {
		//println(formats)
		for (format in formats) {
			try {
				if (format.tryReadInfo(data.duplicate()) == null) continue
				return format.decodeStream(data.duplicate()) ?: continue
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}
		return null
	}

	override suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String) {
		val ext = PathInfo(filename).extensionLC
		val format = formats.firstOrNull { ext in it.extensions }
				?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext'")
		return format.encode(data, out, filename)
	}
}

suspend fun VfsFile.readSoundInfo(formats: AudioFormats = defaultAudioFormats) =
	this.openUse2 { formats.tryReadInfo(this) }

fun AudioFormats.registerStandard(): AudioFormats = this.apply { register(WAV, OGG, MP3) }
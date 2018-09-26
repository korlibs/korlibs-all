package com.soywiz.korau.format

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlin.math.*

interface BaseAudioStream {
	val rate: Int
	val channels: Int
	suspend fun read(out: ShortArray, offset: Int, length: Int): Int
}

open class AudioStream(
	override val rate: Int,
	override val channels: Int
) : BaseAudioStream {
	override suspend fun read(out: ShortArray, offset: Int, length: Int): Int {
		return 0
	}

	suspend fun toData(): AudioData {
		val out = AudioBuffer()
		val buffer = ShortArray(1024)
		while (true) {
			val read = read(buffer, 0, buffer.size)
			if (read <= 0) break
			out.write(buffer, 0, read)
		}
		return AudioData(rate, channels, out.toShortArray())
	}

	companion object {
		fun generator(rate: Int, channels: Int, gen: suspend () -> ShortArray?): AudioStream {
			return object : AudioStream(rate, channels) {
				var chunk: ShortArray = shortArrayOf()
				var pos = 0
				val available get() = chunk.size - pos
				val chunks = LinkedList<ShortArray>()

				override suspend fun read(out: ShortArray, offset: Int, length: Int): Int {
					while (available <= 0) {
						if (chunks.isEmpty()) chunks += gen() ?: return 0
						chunk = chunks.removeFirst()
						pos = 0
					}
					val read = min(length, available)
					arraycopy(chunk, pos, out, offset, read)
					pos += read
					return read
				}
			}
		}
	}
}

suspend fun AudioStream.play(bufferSeconds: Double = 0.1) = nativeSoundProvider.play(this, bufferSeconds)
suspend fun BaseAudioStream.play(bufferSeconds: Double = 0.1) = nativeSoundProvider.play(this, bufferSeconds)

suspend fun VfsFile.readAudioStream(formats: AudioFormats = defaultAudioFormats) = formats.decodeStream(this.open())

suspend fun VfsFile.writeAudio(data: AudioData, formats: AudioFormats = defaultAudioFormats) =
	this.openUse2(VfsOpenMode.CREATE_OR_TRUNCATE) {
		formats.encode(data, this, this@writeAudio.basename)
	}

// @TODO: Problem with Kotlin.JS. Fails in runtime returning kotlin.Unit.
// @TODO: BUG in Kotlin.JS. Fails in runtime returning kotlin.Unit.
/*
suspend inline fun <T> VfsFile.openUse2(
	mode: VfsOpenMode = VfsOpenMode.READ,
	noinline callback: suspend AsyncStream.() -> T
): T {
	return open(mode).use { callback.await(this) }
}
*/
// @TODO: BUG in Kotlin.JS. Fails in runtime returning kotlin.Unit.
/*
suspend inline fun <T> VfsFile.openUse2(
	mode: VfsOpenMode = VfsOpenMode.READ,
	noinline callback: suspend AsyncStream.() -> T
): T {
	//return open(mode).use { callback.await(this) }
	val s = open(mode)
	try {
		return callback.await(s)
	} finally {
		s.close()
	}
}
*/

// @TODO: Works in Kotlin.JS
suspend fun <T> VfsFile.openUse2(
	mode: VfsOpenMode = VfsOpenMode.READ,
	callback: suspend AsyncStream.() -> T
): T {
	//return open(mode).use { callback.await(this) }
	val s = open(mode)
	try {
		return callback.await(s)
	} finally {
		s.close()
	}
}

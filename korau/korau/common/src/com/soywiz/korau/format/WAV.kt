@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korau.format

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

object WAV : AudioFormat("wav") {
	data class Chunk(val type: String, val data: AsyncStream)
	data class ProcessedChunk(val type: String, val data: AsyncStream, val extra: Any)

	override suspend fun tryReadInfo(data: AsyncStream): Info? = ignoreErrors {
		parse(data) { }
	}

	override suspend fun decodeStream(data: AsyncStream): AudioStream? {
		var fmt = Fmt()
		var buffer = MemorySyncStream().toAsync()
		parse(data) {
			val extra = it.extra
			when (extra) {
				is Fmt -> fmt = extra
			}
			if (it.type == "data") {
				buffer = it.data
			}
		}

		val bytesPerSample: Int = fmt.bitsPerSample / 8

		return object : AudioStream(fmt.samplesPerSec, fmt.channels) {
			override suspend fun read(out: ShortArray, offset: Int, length: Int): Int {
				val bytes = FastByteArrayInputStream(buffer.readBytesUpTo(length * bytesPerSample))
				val availableSamples = bytes.length / bytesPerSample
				when (bytesPerSample) {
					2 -> {
						val temp = bytes.readShortArray_le(availableSamples) // @TODO: avoid allocations
						arraycopy(temp, 0, out, offset, temp.size)
					}
					3 -> {
						for (n in 0 until length) {
							if (bytes.available < 3) return n
							out[offset + n] = (bytes.readS24_le() ushr 8).toShort()
						}
					}
					else -> invalidOp("Unsupported bytesPerSample=$bytesPerSample")
				}
				return availableSamples
			}
		}
	}

	override suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String) {
		// HEADER
		out.writeString("RIFF")
		out.write32_le(0x24 + data.samples.size * 2) // length
		out.writeString("WAVE")

		// FMT
		out.writeString("fmt ")
		out.write32_le(0x10)
		out.write16_le(1) // PCM
		out.write16_le(data.channels) // Channels
		out.write32_le(data.rate) // SamplesPerSec
		out.write32_le(data.rate * data.channels * 2) // AvgBytesPerSec
		out.write16_le(2) // BlockAlign
		out.write16_le(16) // BitsPerSample

		// DATA
		out.writeString("data")
		out.write32_le(data.samples.size * 2)
		out.writeShortArray_le(data.samples)
	}

	data class Fmt(
		var formatTag: Int = -1, // CM = 1 (i.e. Linear quantization) Values other than 1 indicate some form of compression.
		var channels: Int = 2, // Mono = 1, Stereo = 2, etc.
		var samplesPerSec: Int = 44100, // 8000, 44100, etc.
		var avgBytesPerSec: Long = 0L, // == SampleRate * NumChannels * BitsPerSample/8
		var blockAlign: Int = 0, // == NumChannels * BitsPerSample/8 The number of bytes for one sample including all channels. I wonder what happens when this number isn't an integer?
		var bitsPerSample: Int = 0      // 8 bits = 8, 16 bits = 16, etc.
	)

	suspend fun parse(data: AsyncStream, handle: (ProcessedChunk) -> Unit): Info {
		val fmt = Fmt()
		var dataSize = 0L

		riff(data) {
			val (type, d2) = this
			val d = d2.duplicate()
			var cdata: Any = Unit
			when (type) {
				"fmt " -> {
					fmt.formatTag = d.readS16_le()
					fmt.channels = d.readS16_le()
					fmt.samplesPerSec = d.readS32_le()
					fmt.avgBytesPerSec = d.readU32_le()
					fmt.blockAlign = d.readS16_le()
					fmt.bitsPerSample = d.readS16_le()
					cdata = fmt
				}
				"data" -> {
					dataSize += d.getLength()
					cdata = d
				}
				else -> Unit
			}
			handle(ProcessedChunk(this.type, this.data, cdata))
		}
		if (fmt.formatTag < 0) invalidOp("Couldn't find RIFF 'fmt ' chunk")

		return Info(
			lengthInMicroseconds = (dataSize * 1000 * 1000) / fmt.avgBytesPerSec,
			channels = fmt.channels
		)
	}

	suspend fun riff(data: AsyncStream, handler: suspend Chunk.() -> Unit) {
		val s2 = data.duplicate()
		val magic = s2.readString(4)
		val length = s2.readS32_le()
		val magic2 = s2.readString(4)
		if (magic != "RIFF") invalidOp("Not a RIFF file but '$magic'")
		if (magic2 != "WAVE") invalidOp("Not a RIFF + WAVE file")
		val s = s2.readStream(length - 4)
		while (!s.eof()) {
			val type = s.readString(4)
			val size = s.readS32_le()
			val d = s.readStream(size)
			handler.await(Chunk(type, d))
		}
	}
}

suspend fun AudioData.toWav() = WAV.encodeToByteArray(this)
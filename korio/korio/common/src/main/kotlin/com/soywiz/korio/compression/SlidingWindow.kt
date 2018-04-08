package com.soywiz.korio.compression

import com.soywiz.kmem.*
import com.soywiz.korio.stream.*

class SlidingWindow(val nbits: Int) {
	val data = ByteArray(1 shl nbits)
	val mask = data.size - 1
	var pos = 0

	fun get(offset: Int): Int {
		return data[(pos - offset) and mask].toInt() and 0xFF
	}

	fun put(value: Int) {
		data[pos] = value.toByte()
		pos = (pos + 1) and mask
	}

	// @TODO: Optimize with buffering and copying

	suspend fun getPutCopyOut(out: AsyncOutputStream, distance: Int, length: Int) {
		for (n in 0 until length) {
			val v = get(distance)
			out.write8(v)
			put(v)
		}
	}

	suspend fun putOut(out: AsyncOutputStream, bytes: ByteArray, offset: Int, len: Int) {
		out.write(bytes, offset, len)
		for (n in 0 until len) put(bytes[offset + n].toUnsigned())
	}

	suspend fun putOut(out: AsyncOutputStream, byte: Byte) {
		out.write8(byte.toUnsigned())
		put(byte.toUnsigned())
	}
}

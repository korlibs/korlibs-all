package com.soywiz.kmedialayer

import kotlin.math.*

class ByteArrayOutputStream {
	private var pos = 0
	private var data = ByteArray(1024)
	val size get() = pos

	private fun ensure(count: Int): ByteArrayOutputStream {
		if (pos + count > data.size) {
			data = data.copyOf(max(pos + count, data.size * 2))
		}
		return this
	}

	private inline fun byte(v: Number) = run { data[pos++] = v.toByte() }
	fun u8(v: Int) = ensure(1).apply { byte(v) }
	fun u16_le(v: Int) = ensure(2).apply { byte(v shr 0); byte(v shr 8) }
	fun u32_le(v: Int) = ensure(4).apply { byte(v shr 0); byte(v shr 8); byte(v shr 16); byte(v shr 24) }
	fun bytes(data: ByteArray) = ensure(data.size).apply { for (n in 0 until data.size) byte(data[n]) }

	fun toByteArray(): ByteArray {
		return data.copyOf(pos)
	}

	inline fun build(builder: ByteArrayOutputStream.() -> Unit): ByteArray {
		builder(this)
		return toByteArray()
	}
}

inline fun buildByteArray(builder: ByteArrayOutputStream.() -> Unit): ByteArray = ByteArrayOutputStream().build(builder)

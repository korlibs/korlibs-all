package com.soywiz.kgl

import com.soywiz.kmem.*

fun KmlNativeBuffer.toAsciiString(): String {
	var out = ""
	for (n in 0 until mem.size) {
		val b = getByte(n)
		if (b == 0.toByte()) break
		out += b.toChar()
	}
	return out
}

fun KmlNativeBuffer.putAsciiString(str: String): KmlNativeBuffer {
	var n = 0
	for (c in str) {
		if (mem.size >= n) setByte(n++, c.toByte())
	}
	if (mem.size >= n) setByte(n++, 0.toByte())
	return this
}

fun kmlByteBufferOf(vararg values: Byte) =
	KmlNativeBuffer(values.size * 1).apply { for (n in 0 until values.size) this.setByte(n, values[n]) }

fun kmlShortBufferOf(vararg values: Short) =
	KmlNativeBuffer(values.size * 2).apply { for (n in 0 until values.size) this.setShort(n, values[n]) }

fun kmlIntBufferOf(vararg values: Int) =
	KmlNativeBuffer(values.size * 4).apply { for (n in 0 until values.size) this.setInt(n, values[n]) }

fun kmlFloatBufferOf(vararg values: Float) =
	KmlNativeBuffer(values.size * 4).apply { for (n in 0 until values.size) this.setFloat(n, values[n]) }

inline fun <T> DataBufferAlloc(size: Int, callback: (KmlNativeBuffer) -> T): T {
	val buffer = KmlNativeBuffer(size)
	try {
		return callback(buffer)
	} finally {
		//buffer.dispose()
	}
}

fun <T> IntArray.toTempBuffer(callback: (KmlNativeBuffer) -> T): T {
	return kmlNativeBuffer(this.size) { buffer: KmlNativeBuffer ->
		val ints = buffer.arrayInt
		for (n in this.indices) ints[n] = this[n]
		callback(buffer)
	}
}
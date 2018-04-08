package com.soywiz.korio.compression

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
}

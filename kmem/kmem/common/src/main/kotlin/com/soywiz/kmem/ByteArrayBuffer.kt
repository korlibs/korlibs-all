package com.soywiz.kmem

import kotlin.math.*

class ByteArrayBuffer(var data: ByteArray, size: Int = data.size, val allowGrow: Boolean = true) {
	constructor(initialCapacity: Int = 4096) : this(ByteArray(initialCapacity), 0)

	private var _size: Int = size
	var size: Int
		get() = _size
		set(len) {
			ensure(len + 1)
			_size = len
		}

	private fun ensure(expected: Int) {
		if (data.size < expected) {
			if (!allowGrow) throw RuntimeException("ByteArrayBuffer configured to not grow!")
			data = data.copyOf(max(expected, (data.size + 7) * 5))
		}
	}

	fun append(ba: ByteArray, offset: Int, len: Int) {
		val ssize = size
		size += len
		arraycopy(ba, offset, data, ssize, len)
		//for (n in 0 until len) data[ssize + n] = ba[offset + n]
	}

	fun append(v: Byte) {
		size++
		data[size - 1] = v
	}

	fun clear() {
		size = 0
	}

	fun toByteArraySlice(position: Long = 0) = ByteArraySlice(data, position.toInt(), size)
	fun toByteArray(): ByteArray = data.copyOf(size)
}
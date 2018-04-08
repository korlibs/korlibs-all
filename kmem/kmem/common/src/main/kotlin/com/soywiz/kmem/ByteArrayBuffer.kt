package com.soywiz.kmem

import kotlin.math.*

class ByteArrayBuffer(var data: ByteArray, size: Int = data.size, val allowGrow: Boolean = true) {
	constructor(initialCapacity: Int = 4096) : this(ByteArray(initialCapacity), 0)

	private var _size: Int = size
	var size: Int
		get() = _size
		set(len) {
			ensure(len)
			_size = len
		}

	fun ensure(expected: Int) {
		if (data.size < expected) {
			if (!allowGrow) throw RuntimeException("ByteArrayBuffer configured to not grow!")
			data = data.copyOf(max(expected, (data.size + 7) * 5))
		}
		_size = max(size, expected)
	}

	fun toByteArraySlice(position: Long = 0) = ByteArraySlice(data, position.toInt(), size)
	fun toByteArray(): ByteArray = data.copyOf(size)
}
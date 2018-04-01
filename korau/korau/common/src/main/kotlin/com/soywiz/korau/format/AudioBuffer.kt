package com.soywiz.korau.format

import com.soywiz.kmem.*

open class AudioBuffer {
	var buffer = ShortArray(0)
	var bufferlen = 0

	fun ensure(len: Int) {
		if (this.bufferlen + len > buffer.size) {
			buffer = buffer.copyOf((this.bufferlen + len) * 2)
		}
	}

	fun write(data: ShortArray, offset: Int, len: Int) {
		if (len <= 0) return
		ensure(len)
		arraycopy(data, offset, this.buffer, this.bufferlen, len)
		this.bufferlen += len
	}

	fun toShortArray() = buffer.copyOf(bufferlen)
}
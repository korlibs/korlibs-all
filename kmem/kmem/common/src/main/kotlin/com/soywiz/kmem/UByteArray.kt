package com.soywiz.kmem

@Suppress("NOTHING_TO_INLINE")
class UByteArray(val data: ByteArray) {
	constructor(size: Int) : this(ByteArray(size))

	companion object {
		inline operator fun invoke(size: Int, init: (index: Int) -> Int) =
			UByteArray(ByteArray(size) { init(it).toByte() })
	}

	val size: Int = data.size
	inline operator fun get(n: Int) = this.data[n].toInt() and 0xFF
	inline operator fun set(n: Int, v: Int) = Unit.let { this.data[n] = v.toByte() }

	@Deprecated("", ReplaceWith("this[n]"))
	fun getu(n: Int) = this[n]
}
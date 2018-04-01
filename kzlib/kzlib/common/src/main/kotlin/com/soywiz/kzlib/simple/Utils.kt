package com.soywiz.kzlib.simple

fun ByteArray.toASCIIString(): String {
	var out = ""
	for (n in 0 until size) out += this[n].toChar()
	return out
}

interface SyncOutputStream {
	fun write8(v: Int)
}

class ByteArraySyncOutputStream : SyncOutputStream {
	var out = ByteArray(1024)
	var size = 0
	private val available get() = capacity - size
	val capacity get() = out.size

	private fun ensure(count: Int) {
		if (available < count) {
			out = out.copyOf(out.size * 2 + count)
		}
	}

	override fun write8(v: Int) {
		ensure(1)
		out[size++] = v.toByte()
	}

	fun toByteArray() = out.copyOf(size)
}

inline fun MemorySyncStreamToByteArray(callback: ByteArraySyncOutputStream.() -> Unit): ByteArray =
	ByteArraySyncOutputStream().apply(callback).toByteArray()

private fun unhex(c: Char): Int = when (c) {
	in '0'..'9' -> 0 + (c - '0')
	in 'a'..'f' -> 10 + (c - 'a')
	in 'A'..'F' -> 10 + (c - 'A')
	else -> throw RuntimeException("Illegal HEX character $c")
}
private val HEX_DIGITS = "0123456789ABCDEF"

val String.unhex: ByteArray
	get() {
		val str = this
		val out = ByteArray(str.length / 2)
		var m = 0
		for (n in 0 until out.size) {
			out[n] = ((unhex(str[m++]) shl 4) or unhex(str[m++])).toByte()
		}
		return out
	}

val Int.hex32: String get() {
	var out = ""
	for (n in 0 until 8) out += HEX_DIGITS[(this ushr ((7 - n) * 4)) and 0xF]
	return out
}
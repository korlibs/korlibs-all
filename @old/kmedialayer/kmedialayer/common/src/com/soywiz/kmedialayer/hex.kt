package com.soywiz.kmedialayer

private fun unhex(c: Char): Int = when (c) {
	in '0'..'9' -> 0 + (c - '0')
	in 'a'..'f' -> 10 + (c - 'a')
	in 'A'..'F' -> 10 + (c - 'A')
	else -> throw RuntimeException("Illegal HEX character $c")
}

internal fun unhex(str: String): ByteArray {
	val out = ByteArray(str.length / 2)
	var m = 0
	for (n in 0 until out.size) {
		out[n] = ((unhex(str[m++]) shl 4) or unhex(str[m++])).toByte()
	}
	return out
}

private val HEX_DIGITS = "0123456789abcdef"

fun Int.toHexString(): String {
	var v = this
	var out = ""
	while (v != 0) {
		val digit = (v and 0xF)
		out += HEX_DIGITS[digit]
		v = v ushr 4
	}
	return out.reversed()
}

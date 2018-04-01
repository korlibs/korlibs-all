package com.soywiz.kzlib

fun String.toSimpleByteArray(): ByteArray {
	val out = ByteArray(this.length)
	for (n in 0 until length) out[n] = this[n].toByte()
	return out
}

fun ByteArray.toSimpleString(): String {
	val out = StringBuilder()
	for (n in 0 until size) out.append(this[n].toChar())
	return out.toString()
}
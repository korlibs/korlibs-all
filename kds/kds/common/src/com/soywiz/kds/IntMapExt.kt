package com.soywiz.kds

fun <T> Map<Int, T>.toIntMap(): IntMap<T> {
	val out = IntMap<T>()
	for ((k, v) in this) out[k] = v
	return out
}

package com.soywiz.korio.util

inline fun <T, R : Any> Iterable<T>.computeRle(callback: (T) -> R): List<Pair<R, Int>> {
	var first = true
	var count = 0
	lateinit var lastRes: R
	val out = arrayListOf<Pair<R, Int>>()
	for (it in this) {
		val current = callback(it)
		if (!first) {
			if (current != lastRes) {
				out += lastRes to count
				count = 0
			}
		}
		lastRes = current
		first = false
		count++
	}
	if (count > 0) {
		out += lastRes to count
	}
	return out
}

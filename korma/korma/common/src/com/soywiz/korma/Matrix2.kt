package com.soywiz.korma

class Matrix2(
	val data: FloatArray = floatArrayOf(
		1f, 0f,
		0f, 1f
	)
) {
	fun index(x: Int, y: Int) = y * 2 + x
	operator fun get(x: Int, y: Int): Float = data[index(x, y)]
	operator fun set(x: Int, y: Int, value: Float) = run { data[index(x, y)] = value }
}

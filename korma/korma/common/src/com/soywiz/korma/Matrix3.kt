package com.soywiz.korma

class Matrix3(
	val data: FloatArray = floatArrayOf(
		1f, 0f, 0f,
		0f, 1f, 0f,
		0f, 0f, 1f
	)
) {
	companion object {
	    operator fun invoke(vararg data: Float) = Matrix3(data)
	}

	fun index(x: Int, y: Int) = y * 3 + x
	operator fun get(x: Int, y: Int): Float = data[index(x, y)]
	operator fun set(x: Int, y: Int, value: Float) = run { data[index(x, y)] = value }

	operator fun times(value: Float) = Matrix3(FloatArray(9) { data[it] * value })
}

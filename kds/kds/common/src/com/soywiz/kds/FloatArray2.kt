package com.soywiz.kds

class FloatArray2(val width: Int, val height: Int, val data: FloatArray = FloatArray(width * height)) {
	fun index(x: Int, y: Int) = y * width + x
	operator fun get(x: Int, y: Int): Float = data[index(x, y)]
	operator fun set(x: Int, y: Int, value: Float) = run { data[index(x, y)] = value }
}

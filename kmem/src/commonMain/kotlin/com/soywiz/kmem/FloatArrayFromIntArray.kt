package com.soywiz.kmem

class FloatArrayFromIntArray(val base: IntArray) {
	operator fun get(i: Int) = Float.fromBits(base[i])
	operator fun set(i: Int, v: Float) = run { base[i] = v.toRawBits() }
}

fun IntArray.asFloatArray(): FloatArrayFromIntArray = FloatArrayFromIntArray(this)

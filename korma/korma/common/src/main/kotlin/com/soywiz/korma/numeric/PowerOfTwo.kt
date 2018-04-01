package com.soywiz.korma.numeric

object PowerOfTwo {
	fun nextPowerOfTwo(value: Int): Int {
		var v = value
		v--
		v = v or (v shr 1)
		v = v or (v shr 2)
		v = v or (v shr 4)
		v = v or (v shr 8)
		v = v or (v shr 16)
		v++
		return v
	}
}

val Int.isPowerOfTwo: Boolean get() = PowerOfTwo.nextPowerOfTwo(this) == this
val Int.nextPowerOfTwo: Int get() = PowerOfTwo.nextPowerOfTwo(this)
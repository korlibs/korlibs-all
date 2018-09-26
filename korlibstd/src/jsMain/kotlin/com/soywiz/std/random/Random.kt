package com.soywiz.std.random

actual object Random {
	actual fun nextInt(): Int = nextInt16() or (nextInt16() shl 16)

	@Suppress("DEPRECATION")
	private fun nextInt16(): Int = kotlin.math.round((kotlin.random.Random.nextDouble() * 0xFFFF)).toInt()
}

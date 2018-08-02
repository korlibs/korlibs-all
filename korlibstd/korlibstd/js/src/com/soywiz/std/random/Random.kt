package com.soywiz.std.random

import kotlin.js.*

actual object Random {
	actual fun nextInt(): Int = nextInt16() or (nextInt16() shl 16)

	@Suppress("DEPRECATION")
	private fun nextInt16(): Int = Math.round((Math.random() * 0xFFFF))
}
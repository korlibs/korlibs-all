package com.soywiz.std.random

actual object Random {
	private val jrand = java.util.Random()
	actual fun nextInt(): Int {
		return jrand.nextInt()
	}
}
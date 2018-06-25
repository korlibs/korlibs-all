package com.soywiz.korma.random

actual object BaseRand {
	init {
		platform.posix.srand(platform.posix.time(null).toInt())
	}

	actual fun random(): Double = (platform.posix.rand() and 0x7FFFFFFF).toDouble() / (0x7FFFFFFF).toDouble()
}

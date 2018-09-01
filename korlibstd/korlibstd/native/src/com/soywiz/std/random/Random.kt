package com.soywiz.std.random

import kotlinx.cinterop.*
import platform.posix.*

actual object Random {
	///////////////
	// gettimeofday not defined on mingw!
	///////////////

	//private fun __microClock(): Long = memScoped {
	//	val timeVal = alloc<timeval>()
	//	gettimeofday(timeVal.ptr, null)
	//	val sec = timeVal.tv_sec
	//	val usec = timeVal.tv_usec
	//	(sec.toLong() shl 32) or (usec.toLong() shl 0)
	//}

	init {
		//srand(__microClock().toInt())
		srand(kotlin.system.getTimeMicros().toUInt())
	}

	actual fun nextInt(): Int = rand()
}

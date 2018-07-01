package com.soywiz.klogger

import platform.posix.*
import konan.internal.GC

actual inline fun Console.error(vararg msg: Any?) {
	//println(msg[0])
	//fflush(__stdoutp)
	//GC.collect()
	println(msg.joinToString(", "))
	fflush(__stdoutp)
	GC.collect()
}

actual inline fun Console.log(vararg msg: Any?) {
	//println(msg[0])
	//fflush(__stdoutp)
	//GC.collect()
	println(msg.joinToString(", "))
	fflush(__stdoutp)
	GC.collect()
}

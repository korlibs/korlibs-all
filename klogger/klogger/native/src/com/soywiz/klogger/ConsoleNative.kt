package com.soywiz.klogger

import platform.posix.*

actual inline fun Console.error(vararg msg: Any?) {
	println(msg.joinToString(", "))
	fflush(__stdoutp)
}

actual inline fun Console.log(vararg msg: Any?) {
	println(msg.joinToString(", "))
	fflush(__stdoutp)
}

actual typealias ConsoleThreadLocal = konan.ThreadLocal

package com.soywiz.klogger

expect object KloggerConsole {
	fun error(msg: Any?): Unit
	fun log(msg: Any?): Unit
}

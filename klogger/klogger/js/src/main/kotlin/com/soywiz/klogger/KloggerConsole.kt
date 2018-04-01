package com.soywiz.klogger

actual object KloggerConsole {
	actual fun error(msg: Any?) {
		console.error(msg)
	}

	actual fun log(msg: Any?) {
		console.log(msg)
	}
}
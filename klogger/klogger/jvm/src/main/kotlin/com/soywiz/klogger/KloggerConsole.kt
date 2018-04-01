package com.soywiz.klogger

actual object KloggerConsole {
	actual fun error(msg: Any?) {
		System.err.println("#" + Thread.currentThread().id + ": " + msg)
	}

	actual fun log(msg: Any?) {
		System.out.println("#" + Thread.currentThread().id + ": " + msg)
	}
}
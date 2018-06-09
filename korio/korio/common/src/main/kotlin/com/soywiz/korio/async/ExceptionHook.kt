package com.soywiz.korio.async

import com.soywiz.klogger.*
import com.soywiz.korio.lang.*

object ExceptionHook {
	var show = false

	fun <T : Throwable> hook(exception: T): T {
		if (show) {
			Logger("ExceptionHook").error { "$exception" }
			exception.printStackTrace()
		}
		return exception
	}
}
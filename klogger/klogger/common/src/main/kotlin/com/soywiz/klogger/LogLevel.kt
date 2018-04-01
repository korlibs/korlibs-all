package com.soywiz.klogger

enum class LogLevel(val index: Int) {
	NONE(0),
	FATAL(1),
	ERROR(2),
	WARN(3),
	INFO(4),
	DEBUG(5),
	TRACE(6)
}

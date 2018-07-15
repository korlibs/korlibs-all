package com.soywiz.korio.async

import kotlinx.coroutines.experimental.*

suspend fun <T> withOptTimeout(ms: Int?, name: String = "timeout", callback: suspend () -> T): T {
	if (ms == null) return callback()
	return withTimeout(ms) { callback() }
}

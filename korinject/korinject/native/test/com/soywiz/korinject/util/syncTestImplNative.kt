package com.soywiz.korinject.util

import kotlin.coroutines.experimental.*

actual fun syncTestImpl(ignoreJs: Boolean, block: suspend () -> Unit) {
	var e: Throwable? = null
	var done = false

	block.startCoroutine(object : OldContinuationAdaptor<Unit>() {
		override val context: CoroutineContext = EmptyCoroutineContext
		override fun resume(value: Unit) {
			done = true
		}

		override fun resumeWithException(exception: Throwable) {
			done = true
			e = exception
		}
	})

	while (!done) platform.posix.usleep(1000)
	e?.let { throw it }
}
package com.soywiz.korinject.util

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.startCoroutine

actual fun syncTestImpl(ignoreJs: Boolean, block: suspend () -> Unit) {
	var e: Throwable? = null
	var done = false

	block.startCoroutine(object : Continuation<Unit> {
		override val context: CoroutineContext = EmptyCoroutineContext
		override fun resume(value: Unit) = run { done = true }
		override fun resumeWithException(exception: Throwable) = run { done = true; e = exception }
	})

	while (!done) Thread.sleep(1L)
	e?.let { throw it }
}
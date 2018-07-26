package com.soywiz.korinject.util

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

actual fun syncTestImpl(ignoreJs: Boolean, block: suspend () -> Unit) {
	var e: Throwable? = null
	var done = false

	block.startCoroutine(object : Continuation<Unit> {
		override val context: CoroutineContext = EmptyCoroutineContext
		override fun resumeWith(result: SuccessOrFailure<Unit>) {
			done = true
			e = result.exceptionOrNull()
		}
	})

	while (!done) Thread.sleep(1L)
	e?.let { throw it }
}

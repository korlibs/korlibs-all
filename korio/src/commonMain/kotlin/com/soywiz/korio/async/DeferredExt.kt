package com.soywiz.korio.async

import com.soywiz.std.coroutine.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

fun <T> CompletableDeferred<T>.toContinuation(context: CoroutineContext, job: Job? = null): Continuation<T> {
	val deferred = CompletableDeferred<T>(job)
	return object : OldContinuationAdaptor<T>() {
		override val context: CoroutineContext = context
		override fun resume(value: T) {
			deferred.complete(value)
		}

		override fun resumeWithException(exception: Throwable) {
			deferred.completeExceptionally(exception)
		}
	}
}

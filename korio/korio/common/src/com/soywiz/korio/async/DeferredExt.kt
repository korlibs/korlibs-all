package com.soywiz.korio.async

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*

fun <T> CompletableDeferred<T>.toContinuation(context: CoroutineContext, job: Job? = null): Continuation<T> {
	val deferred = CompletableDeferred<T>(job)
	return object : Continuation<T> {
		override val context: CoroutineContext = context
		override fun resume(value: T): Unit = run { deferred.complete(value) }
		override fun resumeWithException(exception: Throwable): Unit = run { deferred.completeExceptionally(exception) }
	}
}

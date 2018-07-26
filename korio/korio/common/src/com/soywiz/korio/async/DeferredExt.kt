package com.soywiz.korio.async

import kotlinx.coroutines.*
import kotlin.coroutines.*

fun <T> CompletableDeferred<T>.toContinuation(context: CoroutineContext, job: Job? = null): Continuation<T> {
	val deferred = CompletableDeferred<T>(job)
	return object : Continuation<T> {
		override val context: CoroutineContext = context
		override fun resumeWith(result: SuccessOrFailure<T>) {
			if (result.isSuccess) {
				deferred.complete(result.getOrThrow())
			} else {
				deferred.completeExceptionally(result.exceptionOrNull()!!)
			}
		}
	}
}

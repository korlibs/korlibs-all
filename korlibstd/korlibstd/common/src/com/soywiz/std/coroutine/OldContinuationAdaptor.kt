package com.soywiz.std.coroutine

import kotlin.coroutines.*

abstract class OldContinuationAdaptor<T> : Continuation<T> {
	final override fun resumeWith(result: SuccessOrFailure<T>) {
		if (result.isSuccess) {
			resume(result.getOrThrow())
		} else {
			resumeWithException(result.exceptionOrNull()!!)
		}
	}

	abstract fun resume(value: T)
	abstract fun resumeWithException(exception: Throwable)
}

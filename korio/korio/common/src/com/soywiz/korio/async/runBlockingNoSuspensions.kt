package com.soywiz.korio.async

import com.soywiz.korio.error.*
import kotlin.coroutines.experimental.*

/**
 * Allows to execute a suspendable block as long as you can ensure no suspending will happen at all..
 */
fun <T : Any> runBlockingNoSuspensions(callback: suspend () -> T): T {
	var completed = false
	lateinit var result: T
	var resultEx: Throwable? = null
	var suspendCount = 0
	callback.startCoroutine(object : Continuation<T> {
		override val context: CoroutineContext = object : ContinuationInterceptor {
			override val key: CoroutineContext.Key<*> = ContinuationInterceptor.Key

			override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
				suspendCount++
				return continuation
			}
		}
		override fun resume(value: T) = run { result = value; completed = true }
		override fun resumeWithException(exception: Throwable) = run { resultEx = exception; completed = true }
	})
	if (!completed) invalidOp("ioSync was not completed synchronously! suspendCount=$suspendCount")
	if (resultEx != null) throw resultEx!!
	return result
}

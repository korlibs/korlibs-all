package com.soywiz.korinject.util

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.startCoroutine

val global = js("(typeof global !== 'undefined') ? global : window")

actual fun syncTestImpl(ignoreJs: Boolean, block: suspend () -> Unit) {
	if (ignoreJs) return

	global.testPromise = kotlin.js.Promise<Unit> { resolve, reject ->
		block.startCoroutine(object : Continuation<Unit> {
			override val context: CoroutineContext = EmptyCoroutineContext
			override fun resume(value: Unit) = resolve(Unit)
			override fun resumeWithException(exception: Throwable) = reject(exception)
		})
	}
}
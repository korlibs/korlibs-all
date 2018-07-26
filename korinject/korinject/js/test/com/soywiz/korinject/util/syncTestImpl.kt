package com.soywiz.korinject.util

import kotlin.coroutines.*

val global = js("(typeof global !== 'undefined') ? global : window")

actual fun syncTestImpl(ignoreJs: Boolean, block: suspend () -> Unit) {
	if (ignoreJs) return

	global.testPromise = kotlin.js.Promise<Unit> { resolve, reject ->
		block.startCoroutine(object : Continuation<Unit> {
			override val context: CoroutineContext = EmptyCoroutineContext
			override fun resumeWith(result: SuccessOrFailure<Unit>) {
				if (result.isSuccess) {
					resolve(Unit)
				} else {
					reject(result.exceptionOrNull()!!)
				}
			}
		})
	}
}
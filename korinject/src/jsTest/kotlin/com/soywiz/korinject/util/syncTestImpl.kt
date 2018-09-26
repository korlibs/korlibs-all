package com.soywiz.korinject.util

import com.soywiz.std.coroutine.*
import kotlin.coroutines.*

val global = js("(typeof global !== 'undefined') ? global : window")

actual fun syncTestImpl(ignoreJs: Boolean, block: suspend () -> Unit) {
	if (ignoreJs) return

	global.testPromise = kotlin.js.Promise<Unit> { resolve, reject ->
		block.startCoroutine(object : OldContinuationAdaptor<Unit>() {
			override val context: CoroutineContext = EmptyCoroutineContext
			override fun resume(value: Unit) = resolve(value)
			override fun resumeWithException(exception: Throwable) = reject(exception)
		})
	}
}
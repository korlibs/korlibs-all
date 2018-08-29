package com.soywiz.korio.async

import com.soywiz.korio.lang.*
import com.soywiz.std.coroutine.*
import kotlin.coroutines.*

open class EmptyContinuation(override val context: CoroutineContext) : OldContinuationAdaptor<Unit>() {
	override fun resume(value: Unit) = Unit
	override fun resumeWithException(exception: Throwable) = exception.printStackTrace()

	companion object : EmptyContinuation(EmptyCoroutineContext)
}
package com.soywiz.korio.async

import com.soywiz.korio.lang.*
import kotlin.coroutines.*

open class EmptyContinuation(override val context: CoroutineContext) : OldContinuationAdaptor<Unit>() {
	override fun resume(value: Unit) = Unit
	override fun resumeWithException(exception: Throwable) = exception.printStackTrace()

	companion object : EmptyContinuation(EmptyCoroutineContext)
}
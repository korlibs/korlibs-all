package com.soywiz.korio.async

import com.soywiz.korio.lang.*
import kotlin.coroutines.experimental.*

open class EmptyContinuation(override val context: CoroutineContext) : Continuation<Unit> {
	override fun resume(value: Unit) = Unit
	override fun resumeWithException(exception: Throwable) = exception.printStackTrace()

	companion object : EmptyContinuation(EmptyCoroutineContext)
}
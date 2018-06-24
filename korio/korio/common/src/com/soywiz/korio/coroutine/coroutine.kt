package com.soywiz.korio.coroutine

import com.soywiz.korio.*
import com.soywiz.korio.async.*
import kotlin.coroutines.experimental.*

// @TODO: coroutineContext/suspendCoroutineUninterceptedOrReturn Not supported in kotlin-native 0.7.1
//suspend fun getCoroutineContext(): CoroutineContext = coroutineContext
//suspend fun getCoroutineContext(): CoroutineContext = kotlin.coroutines.experimental.intrinsics.suspendCoroutineUninterceptedOrReturn { it.resume(it.context) }
suspend fun getCoroutineContext(): CoroutineContext = korioSuspendCoroutine<CoroutineContext> { c ->
	c.resume(c.context)
}
suspend fun eventLoop(): EventLoop = getCoroutineContext().eventLoop

val currentThreadId: Long get() = KorioNative.currentThreadId

@Deprecated("Use getCoroutineContext() instead")
suspend fun <T> withCoroutineContext(callback: suspend CoroutineContext.() -> T) = korioSuspendCoroutine<T> { c ->
	callback.startCoroutine(c.context, c)
}

@Deprecated("Use eventLoop() instead")
suspend fun <T> withEventLoop(callback: suspend EventLoop.() -> T) = korioSuspendCoroutine<T> { c ->
	callback.startCoroutine(c.context.eventLoop, c)
}

suspend inline fun <T> korioSuspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T =
	_korioSuspendCoroutine { c ->
		block(c.toEventLoop())
	}

suspend inline fun <T> _korioSuspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T {
	return suspendCoroutine<T> { c: Continuation<T> ->
		block(c)
	}

	// @TODO: Enable just in jtransc that is single-threaded as an optimization?
	//return kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn { c: Continuation<T> ->
	//	val unsafe = UnsafeContinuation(c)
	//	block(unsafe)
	//	unsafe.getResult()
	//}
}

fun <R, T> (suspend R.() -> T).korioStartCoroutine(receiver: R, completion: Continuation<T>) =
	this.startCoroutine(receiver, completion)

fun <T> (suspend () -> T).korioStartCoroutine(completion: Continuation<T>) = this.startCoroutine(completion)
fun <T> (suspend () -> T).korioCreateCoroutine(completion: Continuation<T>): Continuation<Unit> =
	this.createCoroutine(completion)

fun <R, T> (suspend R.() -> T).korioCreateCoroutine(receiver: R, completion: Continuation<T>): Continuation<Unit> =
	this.createCoroutine(receiver, completion)

//private val UNDECIDED: Any? = Any()
//private val RESUMED: Any? = Any()
//
//private class Fail(val exception: Throwable)

//@PublishedApi
//internal class UnsafeContinuation<in T> @PublishedApi internal constructor(private val delegate: Continuation<T>) : Continuation<T> {
//	override val context: CoroutineContext get() = delegate.context
//
//	@Volatile
//	private var result: Any? = UNDECIDED
//
//	override fun resume(value: T) {
//		val result = this.result
//		when {
//			result === UNDECIDED -> {
//				this.result = value
//			}
//			result === COROUTINE_SUSPENDED -> {
//				this.result = RESUMED
//				delegate.resume(value)
//			}
//			else -> throw java.lang.IllegalStateException("Already resumed")
//		}
//	}
//
//	override fun resumeWithException(exception: Throwable) {
//		val result = this.result
//		when {
//			result === UNDECIDED -> {
//				this.result = Fail(exception)
//			}
//			result === kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED -> {
//				this.result = RESUMED
//				delegate.resumeWithException(exception)
//				return
//			}
//			else -> throw java.lang.IllegalStateException("Already resumed")
//		}
//	}
//
//	@PublishedApi
//	internal fun getResult(): Any? {
//		val result = this.result
//		if (result === UNDECIDED) {
//			this.result = kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
//			return kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
//		}
//		when {
//			result === RESUMED -> return kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED // already called continuation, indicate SUSPENDED_MARKER upstream
//			result is Fail -> throw result.exception
//			else -> return result // either SUSPENDED_MARKER or data
//		}
//	}
//}

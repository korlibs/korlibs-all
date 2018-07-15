package com.soywiz.korio.async

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.intrinsics.*
import kotlin.coroutines.experimental.*

//class AsyncQueue(val context: CoroutineContext) {
class AsyncQueue() {
	//constructor() : AsyncQueue(CoroutineContext())

	private var promise: Deferred<Any> = CompletableDeferred(Unit)

	//companion object {
	//	suspend operator fun invoke() = AsyncQueue(getCoroutineContext())
	//}

	suspend operator fun invoke(func: suspend () -> Unit): AsyncQueue = invoke(coroutineContext, func)

	operator fun invoke(context: CoroutineContext, func: suspend () -> Unit): AsyncQueue {
		//operator fun invoke(func: suspend () -> Unit): AsyncQueue {
		val oldPromise = this@AsyncQueue.promise
		val newDeferred = CompletableDeferred<Any>()
		this@AsyncQueue.promise = newDeferred
		oldPromise.invokeOnCompletion {
			func.startCoroutineCancellable(newDeferred.toContinuation(context))
		}
		return this@AsyncQueue
	}

	suspend fun await(func: suspend () -> Unit) {
		invoke(func)
		await()
	}

	suspend fun await() = promise.await()
}

fun AsyncQueue.withContext(ctx: CoroutineContext) = AsyncQueueWithContext(this, ctx)
suspend fun AsyncQueue.withContext() = AsyncQueueWithContext(this, coroutineContext)

class AsyncQueueWithContext(val queue: AsyncQueue, val context: CoroutineContext) {
	operator fun invoke(func: suspend () -> Unit): AsyncQueue = queue.invoke(context, func)
	suspend fun await(func: suspend () -> Unit) = queue.await(func)
	suspend fun await() = queue.await()
}

class AsyncThread {
	private var lastPromise: Deferred<*> = CompletableDeferred(Unit)

	fun cancel(): AsyncThread {
		lastPromise.cancel()
		lastPromise = CompletableDeferred(Unit)
		return this
	}

	suspend fun <T> cancelAndQueue(func: suspend () -> T): T {
		cancel()
		return queue(func)
	}

	suspend fun <T> queue(func: suspend () -> T): T = invoke(func)

	suspend operator fun <T> invoke(func: suspend () -> T): T {
		val ctx = coroutineContext
		val newDeferred = CompletableDeferred<T>()
		lastPromise.invokeOnCompletion {
			func.startCoroutineCancellable(newDeferred.toContinuation(ctx))
		}
		lastPromise = newDeferred
		return newDeferred.await() as T
	}

	suspend fun <T> sync(func: suspend () -> T): Deferred<T> = sync(coroutineContext, func)

	fun <T> sync(context: CoroutineContext, func: suspend () -> T): Deferred<T> {
		val newDeferred = CompletableDeferred<T>()
		lastPromise.invokeOnCompletion {
			func.startCoroutineCancellable(newDeferred.toContinuation(context))
		}
		lastPromise = newDeferred
		return newDeferred
	}
}

//@Deprecated("AsyncQueue", ReplaceWith("AsyncQueue"))
//typealias WorkQueue = AsyncQueue
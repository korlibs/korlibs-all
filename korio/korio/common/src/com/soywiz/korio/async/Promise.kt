package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.korio.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.coroutines.experimental.*

class Promise<T : Any?> : Cancellable {
	class Deferred<T : Any?> {
		val promise = Promise<T>()
		val onCancel = promise.onCancel
		fun resolve(value: T): Unit = run { promise.complete(value, null) }
		fun reject(error: Throwable): Unit = run { promise.complete(null, error) }
		fun toContinuation(coroutineContext: CoroutineContext): CancellableContinuation<T> {
			val deferred = this
			val cc = CancellableContinuation(object : Continuation<T> {
				override val context: CoroutineContext = coroutineContext
				override fun resume(value: T) = deferred.resolve(value)
				override fun resumeWithException(exception: Throwable) = deferred.reject(exception)
			})
			onCancel {
				cc.cancel()
			}
			cc.onCancel {
				promise.cancel()
				cc.cancel()
			}
			return cc
		}
	}

	companion object {
		fun <T> resolved(value: T) = Promise<T>().complete(value, null)
		fun <T> rejected(error: Throwable) = Promise<T>().complete(null, error)

		suspend fun <T> create(callback: suspend (deferred: Deferred<T>) -> Unit): T {
			val deferred = Deferred<T>()
			callback(deferred)
			return deferred.promise.await()
		}
	}

	private var value: T? = null
	private var error: Throwable? = null
	private var done: Boolean = false
	private val resolvedHandlers = Queue<(T) -> Unit>()
	private val rejectedHandlers = Queue<(Throwable) -> Unit>()

	private fun flush() {
		if (!done) return
		if (error != null) {
			while (true) {
				val handler =
					synchronized(rejectedHandlers) { if (rejectedHandlers.size != 0) rejectedHandlers.dequeue() else null }
							?: break
				handler(error ?: RuntimeException())
			}
		} else {
			while (true) {
				val handler =
					synchronized(resolvedHandlers) { if (resolvedHandlers.size != 0) resolvedHandlers.dequeue() else null }
							?: break
				handler(value as T)
			}
		}
	}

	internal fun complete(value: T?, error: Throwable?): Promise<T> {
		if (!this.done) {
			this.value = value
			this.error = error
			this.done = true

			if (error != null && synchronized(resolvedHandlers) { this.rejectedHandlers.size == 0 } && error !is com.soywiz.korio.CancellationException) {
				if (error !is CancellationException) {
					Logger("Promise").error { "## Not handled Promise exception:" }
					error.printStackTrace()
				}
			}

			flush()
		}
		return this
	}

	fun then(resolved: (T) -> Unit): Cancellable {
		synchronized(resolvedHandlers) { resolvedHandlers.enqueue(resolved) }
		flush()
		return Cancellable {
			synchronized(resolvedHandlers) { resolvedHandlers.remove(resolved) }
		}
	}

	fun always(resolved: () -> Unit) = then(resolved = { resolved() }, rejected = { resolved() })

	fun then(resolved: (T) -> Unit, rejected: (Throwable) -> Unit): Cancellable {
		synchronized(resolvedHandlers) { resolvedHandlers.enqueue(resolved) }
		synchronized(rejectedHandlers) { rejectedHandlers.enqueue(rejected) }
		flush()
		return Cancellable {
			synchronized(resolvedHandlers) { resolvedHandlers.remove(resolved) }
			synchronized(rejectedHandlers) { rejectedHandlers.remove(rejected) }
		}
	}

	fun then(c: Continuation<T>): Unit {
		this.then(
			resolved = { c.resume(it) },
			rejected = { c.resumeWithException(it) }
		)
	}

	private val onCancel = Signal<Throwable>()

	override fun cancel(e: Throwable) {
		onCancel(e)
		complete(null, com.soywiz.korio.CancellationException(""))
	}
}

@Deprecated("Use suspendCoroutine instead")
fun <T> Promise(callback: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T> {
	val deferred = Promise.Deferred<T>()
	callback({ deferred.resolve(it) }, { deferred.reject(it) })
	return deferred.promise
}

suspend fun <T> Promise<T>.await(): T = suspendCoroutine(this::then)

suspend fun <T> Iterable<Promise<T>>.await(): List<T> {
	val out = arrayListOf<T>()
	for (p in this) out += p.await()
	return out
}

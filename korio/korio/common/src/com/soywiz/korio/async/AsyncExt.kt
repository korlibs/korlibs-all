package com.soywiz.korio.async

import com.soywiz.klock.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.timeunit.*
import kotlin.coroutines.experimental.*

suspend inline fun <T, R> (suspend T.() -> R).await(receiver: T): R =
	withContext(coroutineContext.dispatcher) { this(receiver) }

suspend inline fun <R> (suspend () -> R).await(): R = withContext(coroutineContext.dispatcher) { this() }

// @TODO: Try to get in subinstance
val CoroutineContext.tryDispatcher: CoroutineDispatcher? get() = this as? CoroutineDispatcher?
val CoroutineContext.dispatcher: CoroutineDispatcher get() = this.tryDispatcher ?: KorioDefaultDispatcher

// @TODO: Do this better! (JS should use requestAnimationFrame)
suspend fun delayNextFrame() = _delayNextFrame()

private suspend fun _delayNextFrame() {
	delay(16)
}

suspend fun CoroutineContext.delayNextFrame() {
	withContext(this) {
		_delayNextFrame()
	}
}

suspend fun CoroutineContext.delay(time: Int) {
	withContext(this) {
		kotlinx.coroutines.experimental.delay(time)
	}
}

suspend fun delay(time: TimeSpan) = delay(time.milliseconds)

suspend fun CoroutineContext.delay(time: TimeSpan) = delay(time.milliseconds)

fun CoroutineContext.animationFrameLoop(callback: suspend () -> Unit): Closeable {
	val job = launch(this) {
		while (true) {
			callback()
			delayNextFrame()
		}
	}
	return Closeable { job.cancel() }
}

interface CoroutineContextHolder {
	val coroutineContext: CoroutineContext
}

class TestCoroutineDispatcher(val parent: CoroutineDispatcher) : CoroutineDispatcher(), Delay {
	var time = 0L

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		parent.dispatcher.dispatch(context, block)
	}

	override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
		this.time += unit.toMillis(time)
		continuation.resume(Unit)
	}
}

suspend fun <T> executeInNewThread(task: suspend () -> T): T = KorioNative.executeInWorker(task)
suspend fun <T> executeInWorker(task: suspend () -> T): T = KorioNative.executeInWorker(task)

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korio.error.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.coroutines.experimental.*

abstract class EventLoopFactory {
	abstract fun createEventLoop(): EventLoop
}

val eventLoopFactoryDefaultImpl: EventLoopFactory get() = KorioNative.eventLoopFactoryDefaultImpl

val tasksInProgress = AtomicInteger(0)

// @TODO: Check CoroutineDispatcher
abstract class EventLoop(val captureCloseables: Boolean) : Closeable {
	constructor() : this(captureCloseables = true)

	val coroutineContext = EventLoopCoroutineContext(this)

	companion object {
		fun main(eventLoop: EventLoop, entry: suspend EventLoop.() -> Unit) {
			if (globalEventLoop == null) {
				globalEventLoop = eventLoop
			}
			tasksInProgress.incrementAndGet()
			eventLoop.setImmediate {
				entry.korioStartCoroutine(eventLoop, object : Continuation<Unit> {
					override val context: CoroutineContext = EventLoopCoroutineContext(eventLoop)

					override fun resume(value: Unit) {
						tasksInProgress.decrementAndGet()
					}

					override fun resumeWithException(exception: Throwable) {
						tasksInProgress.decrementAndGet()
						exception.printStackTrace()
					}
				})
			}
			eventLoop.loop()
		}

		operator fun invoke(entry: suspend EventLoop.() -> Unit): Unit = main(entry)

		fun main(entry: suspend EventLoop.() -> Unit): Unit = main(eventLoopFactoryDefaultImpl.createEventLoop()) {
			this.entry()
		}
	}

	protected abstract fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable

	protected open fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		var cancelled = false
		fun step() {
			setTimeoutInternal(ms, {
				if (!cancelled) {
					callback()
					step()
				}
			})
		}
		step()
		return Closeable { cancelled = true }
	}

	protected open fun setImmediateInternal(handler: () -> Unit): Unit = run { setTimeoutInternal(0, handler) }

	var fps: Double = 60.0

	var lastRequest = 0.0
	protected open fun requestAnimationFrameInternal(callback: () -> Unit): Closeable {
		val step = 1000.0 / fps
		val now = Klock.currentTimeMillisDouble()
		if (lastRequest == 0.0) lastRequest = now
		lastRequest = now
		return setTimeoutInternal((step - (now - lastRequest)).clamp(0.0, step).toInt(), callback)
	}

	open fun loop(): Unit = Unit

	private val closeables = LinkedHashSet<Closeable>()

	private fun Closeable.capture(): Closeable {
		if (captureCloseables) {
			val closeable = this
			closeables += closeable
			return Closeable {
				closeables -= closeable
				closeable.close()
			}
		} else {
			return this
		}
	}

	fun setImmediate(handler: () -> Unit): Unit = setImmediateInternal(handler)
	fun setTimeout(ms: Int, callback: () -> Unit): Closeable = setTimeoutInternal(ms, callback).capture()
	fun setInterval(ms: Int, callback: () -> Unit): Closeable = setIntervalInternal(ms, callback).capture()

	fun setIntervalImmediate(ms: Int, callback: () -> Unit): Closeable {
		setImmediateInternal(callback)
		return setIntervalInternal(ms, callback).capture()
	}

	fun requestAnimationFrame(callback: () -> Unit): Closeable {
		return requestAnimationFrameInternal(callback).capture()
	}

	fun queue(handler: () -> Unit): Unit = setImmediate(handler)

	open fun <T> queueContinuation(continuation: Continuation<T>, result: T): Unit =
		queue { continuation.resume(result) }

	open fun <T> queueContinuationException(continuation: Continuation<T>, result: Throwable): Unit =
		queue { continuation.resumeWithException(result) }

	fun animationFrameLoop(callback: () -> Unit): Closeable {
		var closeable: Closeable? = null
		var step: (() -> Unit)? = null
		var cancelled = false
		step = {
			//println("animationFrameLoop:cancelled:$cancelled")
			if (!cancelled) {
				//println("--callback[")
				callback()
				//println("--callback]")
				closeable = this.requestAnimationFrameInternal(step!!)
			} else {
				//println("--cancelled!")
			}
		}
		step()
		return Closeable {
			cancelled = true
			closeable?.close()
		}.capture()
	}

	override fun close() {
		for (closeable in closeables) {
			closeable.close()
		}
		closeables.clear()
	}

	open val time: Long get() = TimeProvider.now()

	open fun step(ms: Int): Unit = Unit

	suspend fun sleep(ms: Int): Unit = suspendCancellableCoroutine { c ->
		val cc = setTimeout(ms) { c.resume(Unit) }
		c.onCancel {
			cc.close()
			c.resumeWithException(it)
		}
	}

	suspend fun sleepNextFrame(): Unit = suspendCancellableCoroutine { c ->
		val cc = requestAnimationFrame { c.resume(Unit) }
		c.onCancel {
			cc.close()
			c.resumeWithException(it)
		}
	}
}

class EventLoopCoroutineContext(val eventLoop: EventLoop) :
	AbstractCoroutineContextElement(EventLoopCoroutineContext.Key) {
	companion object Key : CoroutineContext.Key<EventLoopCoroutineContext>
}

var globalEventLoop: EventLoop? = null

val CoroutineContext.tryEventLoop: EventLoop? get() = this[EventLoopCoroutineContext.Key]?.eventLoop
val CoroutineContext.eventLoop: EventLoop
	get() = tryEventLoop ?: globalEventLoop ?: invalidOp("No EventLoop associated to this CoroutineContext")
val Continuation<*>.eventLoop: EventLoop get() = this.context.eventLoop

suspend fun CoroutineContext.sleep(ms: Int) = this.eventLoop.sleep(ms)

suspend fun sleepMs(ms: Int): Unit = eventLoop().sleep(ms)
suspend fun sleepNextFrame(): Unit = eventLoop().sleepNextFrame()

object BaseEventLoopFactoryNative : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = BaseEventLoopNative()
}

open class BaseEventLoopNative() : EventLoop(captureCloseables = false) {
	private class AnyObj

	private val lock = AnyObj()
	class Task(val time: Long, val callback: () -> Unit)

	private val timedTasks = PriorityQueue<Task> { a, b ->
		if (a == b) 0 else a.time.compareTo(b.time).compareToChain { if (a == b) 0 else -1 }
	}

	class ImmediateTask {
		var continuation: Continuation<*>? = null
		var continuationResult: Any? = null
		var continuationException: Throwable? = null
		var callback: (() -> Unit)? = null

		fun reset() {
			continuation = null
			continuationResult = null
			continuationException = null
			callback = null
		}
	}

	private val immediateTasksPool = Pool({ it.reset() }) { ImmediateTask() }
	private val immediateTasks = LinkedList<ImmediateTask>()

	override fun setImmediateInternal(handler: () -> Unit) {
		synchronized(lock) {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.callback = handler
			}
		}
	}

	override fun <T> queueContinuation(continuation: Continuation<T>, result: T): Unit {
		synchronized(lock) {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.continuation = continuation
				this.continuationResult = result
			}
		}
	}

	override fun <T> queueContinuationException(continuation: Continuation<T>, result: Throwable): Unit {
		synchronized(lock) {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.continuation = continuation
				this.continuationException = result
			}
		}
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		val task = Task(Klock.currentTimeMillis() + ms, callback)
		synchronized(lock) { timedTasks += task }
		return Closeable { synchronized(timedTasks) { timedTasks -= task } }
	}

	override fun step(ms: Int) {
		timer@ while (true) {
			val startTime = Klock.currentTimeMillis()
			while (true) {
				val currentTime = Klock.currentTimeMillis()
				val item = synchronized(lock) { if (timedTasks.isNotEmpty() && currentTime >= timedTasks.peek().time) timedTasks.remove() else null }
							?: break
				item.callback()
			}
			while (true) {
				if ((Klock.currentTimeMillis() - startTime) >= 50) {
					continue@timer
				}
				val task =
					synchronized(lock) { if (immediateTasks.isNotEmpty()) immediateTasks.removeFirst() else null }
							?: break
				if (task.callback != null) {
					task.callback?.invoke()
				} else if (task.continuation != null) {
					val cont = (task.continuation as? Continuation<Any?>)!!
					if (task.continuationException != null) {
						cont.resumeWithException(task.continuationException!!)
					} else {
						cont.resume(task.continuationResult)
					}
				}
				synchronized(lock) {
					immediateTasksPool.free(task)
				}
			}
			break
		}
	}

	var loopThread: Long = 0L

	override fun loop() {
		loopThread = KorioNative.currentThreadId

		while (synchronized(lock) { immediateTasks.isNotEmpty() || timedTasks.isNotEmpty() } || (tasksInProgress.get() != 0)) {
			step(1)
			KorioNative.Thread_sleep(1L)

			//println("immediateTasks: ${immediateTasks.size}, timedTasks: ${timedTasks.size}, tasksInProgress: ${tasksInProgress.get()}")
		}

		//_workerLazyPool?.shutdownNow()
		//_workerLazyPool?.shutdown()
		//_workerLazyPool?.awaitTermination(5, TimeUnit.SECONDS);
	}
}


//class EventLoopJvmAndCSharp : EventLoop() {
//	override val priority: Int = 1000
//	override val available: Boolean get() = true
//
//	val tasksExecutor = Executors.newSingleThreadExecutor()
//
//	val timer = Timer(true)
//
//	override fun init(): Unit = Unit
//
//	override fun setImmediate(handler: () -> Unit) {
//		tasksExecutor { handler() }
//	}
//
//	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
//		val tt = timerTask { tasksExecutor { callback() } }
//		timer.schedule(tt, ms.toLong())
//		return Closeable { tt.cancel() }
//	}
//
//
//	override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
//		val tt = timerTask { tasksExecutor { callback() } }
//		timer.schedule(tt, ms.toLong(), ms.toLong())
//		return Closeable { tt.cancel() }
//	}
//}

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
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

private var logger = Logger("EventLoop")

// @TODO: Check CoroutineDispatcher
abstract class EventLoop(val captureCloseables: Boolean) : Closeable {
	constructor() : this(captureCloseables = true)

	val coroutineContext = EventLoopCoroutineContext(this)

	companion object {
		fun main(eventLoop: EventLoop, entry: suspend EventLoop.() -> Unit) {
			println(Logger.defaultLevel)
			println(logger.level)
			logger.trace { "EventLoop.main[0]" }
			if (globalEventLoop == null) {
				globalEventLoop = eventLoop
			}
			logger.trace { "EventLoop.main[1]" }
			tasksInProgress.incrementAndGet()
			eventLoop.start()
			logger.trace { "EventLoop.main[2]" }
			println("EventLoop.main[2]")
			eventLoop.setImmediate {
				logger.trace { "EventLoop.main.immediate" }
				println("EventLoop.main.immediate")
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
			logger.trace { "EventLoop.main[3]" }
			eventLoop.loop()
			logger.trace { "EventLoop.main[4]" }
		}

		operator fun invoke(entry: suspend EventLoop.() -> Unit): Unit {
			main(entry)
		}

		fun main(entry: suspend EventLoop.() -> Unit): Unit {
			main(eventLoopFactoryDefaultImpl.createEventLoop()) {
				this.entry()
			}
		}
	}

	protected abstract fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable

	open fun start() {
	}

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
	companion object {
	    val logger = Logger("BaseEventLoopNative")
	}
	private class AnyObj
	private val lock = AnyObj()
	private val immediateTasksPool = Pool({ it.reset() }) { ImmediateTask() }
	private val immediateTasks = LinkedList<ImmediateTask>()
	//private val immediateTasks = arrayListOf<ImmediateTask>()
	var loopThread: Long = 0L

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

		override fun toString(): String = "ImmediateTask(continuation=${continuation != null}, continuationResult=${continuationResult != null}, continuationException=${continuationException != null}, callback=${callback != null})"
	}

	override fun setImmediateInternal(handler: () -> Unit) {
		logger.trace { "setImmediateInternal[0]" }
		//synchronized(lock) {
		run {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.callback = handler
			}
		}
		logger.trace { "setImmediateInternal[1]" }
	}

	override fun <T> queueContinuation(continuation: Continuation<T>, result: T): Unit {
		logger.trace { "queueContinuation[0]" }
		//synchronized(lock) {
		run {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.continuation = continuation
				this.continuationResult = result
			}
		}
		logger.trace { "queueContinuation[1]" }
	}

	override fun <T> queueContinuationException(continuation: Continuation<T>, result: Throwable): Unit {
		logger.trace { "queueContinuationException[0]" }
		//synchronized(lock) {
		run {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.continuation = continuation
				this.continuationException = result
			}
		}
		logger.trace { "queueContinuationException[1]" }
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		logger.trace { "setTimeoutInternal[0]" }
		val task = Task(Klock.currentTimeMillis() + ms, callback)
		//synchronized(lock) {
		run {
			timedTasks += task
		}
		val out = Closeable {
			//synchronized(timedTasks) {
			run {
				timedTasks -= task
			}
		}
		logger.trace { "setTimeoutInternal[1]" }
		return out
	}

	override fun step(ms: Int) {
		logger.trace { "step[0]" }
		timer@ while (true) {
			logger.trace { "step[1]" }
			val startTime = Klock.currentTimeMillis()
			while (true) {
				logger.trace { "step[2]" }
				val currentTime = Klock.currentTimeMillis()
				//val item = synchronized(lock) {
				val item = if (timedTasks.isNotEmpty() && currentTime >= timedTasks.peek().time) timedTasks.remove() else null
				logger.trace { "step[3]" }
				if (item == null) break
				item?.callback()
				logger.trace { "step[4]" }
			}
			while (true) {
				logger.trace { "step[5]" }
				if ((Klock.currentTimeMillis() - startTime) >= 50) {
					continue@timer
				}
				logger.trace { "step[6]" }
				/*
				val task = synchronized(lock) {
					if (immediateTasks.isNotEmpty()) immediateTasks.removeFirst() else null
				} ?: break
				*/
				//val task = (if (immediateTasks.isNotEmpty()) immediateTasks.removeFirst() else null) ?: break
				//val task = if (immediateTasks.isNotEmpty()) immediateTasks.removeFirst() else null
				logger.trace { "step[6b]" }
				val task = if (immediateTasks.isNotEmpty()) {
					logger.trace { "step[6c]" }
					immediateTasks.removeAt(0)
				} else {
					logger.trace { "step[6d]" }
					null
				}
				logger.trace { "step[7.1] $task" }
				if (task != null) {
					logger.trace { "step[7.2]" }
					val callback = task.callback
					val continuation = task.continuation
					val continuationException = task.continuationException
					logger.trace { "step[7.3] $task" }
					if (callback != null) {
						logger.trace { "step[7a]" }
						logger.trace { "step[7aa]" }
						callback()
					} else if (continuation != null) {
						logger.trace { "step[7b]" }
						val cont = (continuation as? Continuation<Any?>)
						logger.trace { "step[7c]" }
						if (continuationException != null) {
							logger.trace { "step[7d]" }
							cont?.resumeWithException(continuationException)
						} else {
							logger.trace { "step[7e]" }
							cont?.resume(task.continuationResult)
						}
						logger.trace { "step[7f]" }
					}
					logger.trace { "step[8]" }
					//synchronized(lock) {
					run {
						immediateTasksPool.free(task)
					}
					logger.trace { "step[9]" }
				}
			}
			logger.trace { "step[10]" }
			break
		}
		logger.trace { "step[11]" }
	}

	override fun loop() {
		logger.trace { "BaseEventLoopNative.loop[0]" }
		loopThread = KorioNative.currentThreadId

		logger.trace { "BaseEventLoopNative.loop[1]" }
		//while (synchronized(lock) { immediateTasks.isNotEmpty() || timedTasks.isNotEmpty() } || (tasksInProgress.get() != 0)) {
		while (run { immediateTasks.isNotEmpty() || timedTasks.isNotEmpty() } || (tasksInProgress.get() != 0)) {
			logger.trace { "BaseEventLoopNative.loop[2]" }
			step(1)
			nativeSleep(1)
			logger.trace { "BaseEventLoopNative.loop[3]" }

			//println("immediateTasks: ${immediateTasks.size}, timedTasks: ${timedTasks.size}, tasksInProgress: ${tasksInProgress.get()}")
		}

		//_workerLazyPool?.shutdownNow()
		//_workerLazyPool?.shutdown()
		//_workerLazyPool?.awaitTermination(5, TimeUnit.SECONDS);
	}

	open fun nativeSleep(time: Int) {
		logger.trace { "BaseEventLoopNative.nativeSleep[0]" }
		KorioNative.Thread_sleep(time.toLong())
		logger.trace { "BaseEventLoopNative.nativeSleep[1]" }
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

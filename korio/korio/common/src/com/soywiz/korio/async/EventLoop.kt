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
import com.soywiz.std.*
import kotlin.coroutines.experimental.*

abstract class EventLoopFactory {
	abstract fun createEventLoop(): EventLoop
}

val eventLoopFactoryDefaultImpl: EventLoopFactory get() = KorioNative.eventLoopFactoryDefaultImpl

// @TODO: Check CoroutineDispatcher
open class EventLoop(val captureCloseables: Boolean) : Closeable {
	constructor() : this(captureCloseables = true)

	val coroutineContext = EventLoopCoroutineContext(this)
	val tasksInProgress = NewAtomicInt(0)

	companion object {
		fun main(eventLoop: EventLoop, entry: suspend EventLoop.() -> Unit) {
			//if (globalEventLoop == null) {
			//	globalEventLoop = eventLoop
			//}
			eventLoop.tasksInProgress.addAndGet(+1)
			eventLoop.start()
			eventLoop.setImmediateDeferred {
				//println("EventLoop entrypoint")
				entry.korioStartCoroutine(eventLoop, object : Continuation<Unit> {
					override val context: CoroutineContext = EventLoopCoroutineContext(eventLoop)

					override fun resume(value: Unit) {
						eventLoop.tasksInProgress.addAndGet(-1)
					}

					override fun resumeWithException(exception: Throwable) {
						eventLoop.tasksInProgress.addAndGet(-1)
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

	private val tasks = PriorityQueue<Task> { a, b -> a.time.compareTo(b.time) }

	inner class Task(val time: Double, val callback: () -> Unit) : Closeable {
		fun mustRun(now: Double) = now >= time

		override fun close() {
			synchronized(tasks) { tasks -= this }
		}

		override fun toString(): String = "Task(time=${time.toLong()})"
	}

	protected open fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		return Task(getTime() + ms, callback).apply {
			synchronized(tasks) { tasks += this }
		}
	}

	open fun step() {
		val start = getTime()
		var now = start
		//println("step: ${tasks.size}, ${tasks.toList()}, ${now.toLong()}")
		while (true) {
			val task = synchronized(tasks) { if (tasks.isNotEmpty()) tasks.first() else null } ?: break

			//println("now=$now, task=$task")
			if (task.mustRun(now)) {
				//println("step: TASK EXEC")
				synchronized(tasks) { tasks -= task }
				//tasks.removeHead()
				task.callback()
				now = getTime()
			} else {
				break
			}

			// Tasks are taking too long, let's continue in the next frame
			if (mustBreak(start, now)) {
				break
			}
		}
	}

	protected open fun mustBreak(start: Double, now: Double): Boolean = now - start >= 250

	open fun start() {
	}

	protected open fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		var cancelled = false
		fun step() {
			setTimeoutInternal(ms) {
				if (!cancelled) {
					callback()
					step()
				}
			}
		}
		step()
		return Closeable { cancelled = true }
	}

	private var insideImmediate by atomicRef(false)
	private val immediateTasks = LinkedList<() -> Unit>()
	protected open fun setImmediateInternal(handler: () -> Unit) {
		// @TODO: Check right thread?
		immediateTasks += handler
		if (!insideImmediate) {
			insideImmediate = true
			try {
				while (immediateTasks.isNotEmpty()) {
					val task = immediateTasks.removeFirst()
					task()
				}
			} finally {
				insideImmediate = false
			}
		}
	}

	var fps: Double = 60.0

	var lastRequest = 0.0
	protected open fun requestAnimationFrameInternal(callback: () -> Unit): Closeable {
		val step = 1000.0 / fps
		val now = getTime()
		if (lastRequest == 0.0) lastRequest = now
		lastRequest = now
		return setTimeoutInternal((step - (now - lastRequest)).clamp(0.0, step).toInt(), callback)
	}

	open fun loop() {
		while (synchronized(tasks) { immediateTasks.isNotEmpty() || tasks.isNotEmpty() } || (tasksInProgress.get() != 0)) {
			step()
			delayFrame()
		}
	}

	protected open fun delayFrame() {
		KorioNative.Thread_sleep(1000L / 60L)
	}

	private val closeables = LinkedHashSet<Closeable>()

	private fun Closeable.capture(): Closeable {
		if (!captureCloseables) return this
		val closeable = this
		closeables += closeable
		return Closeable {
			closeables -= closeable
			closeable.close()
		}
	}

	fun setImmediate(handler: () -> Unit): Unit = setImmediateInternal(handler)
	fun setImmediateDeferred(handler: () -> Unit): Unit = run { setTimeout(0, handler) }
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

	open fun getTime(): Double = Klock.currentTimeMillisDouble()

	open fun step(ms: Int): Unit {
		step()
	}

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

//@ThreadLocal
//var globalEventLoop: EventLoop? = null

val CoroutineContext.tryEventLoop: EventLoop? get() = this[EventLoopCoroutineContext.Key]?.eventLoop
val CoroutineContext.eventLoop: EventLoop
//get() = tryEventLoop ?: globalEventLoop ?: invalidOp("No EventLoop associated to this CoroutineContext")
	get() = tryEventLoop ?: invalidOp("No EventLoop associated to this CoroutineContext")
val Continuation<*>.eventLoop: EventLoop get() = this.context.eventLoop

suspend fun CoroutineContext.sleep(ms: Int) = this.eventLoop.sleep(ms)

suspend fun sleepMs(ms: Int): Unit = eventLoop().sleep(ms)
suspend fun sleepNextFrame(): Unit = eventLoop().sleepNextFrame()

object BaseEventLoopFactoryNative : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoop(captureCloseables = false)
}

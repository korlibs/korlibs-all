package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.timeunit.*
import kotlin.browser.*
import kotlin.coroutines.*

actual val KoruiDispatcher: CoroutineDispatcher get() = if (OS.isNodejs) NodeDispatcher else HtmlDispatcher

private external fun setTimeout(handler: dynamic, timeout: Int = definedExternally): Int
private external fun clearTimeout(handle: Int = definedExternally)

fun TimeUnit.toMillisFaster(time: Long) = when (this) {
	TimeUnit.SECONDS -> time.toInt() * 1000
	TimeUnit.MILLISECONDS -> time.toInt()
}

object NodeDispatcher : CoroutineDispatcher(), Delay, DelayFrame {
	override fun dispatch(context: CoroutineContext, block: Runnable) {
		setTimeout({ block.run() }, 0)
	}

	override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
		val timeout = setTimeout({ with(continuation) { resumeUndispatched(Unit) } }, timeMillis.toInt())
		// Actually on cancellation, but clearTimeout is idempotent
		continuation.invokeOnCancellation {
			clearTimeout(timeout)
		}
	}

	override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
		val timeout = setTimeout({ block.run() }, unit.toMillisFaster(time))
		return object : DisposableHandle {
			override fun dispose() {
				clearTimeout(timeout)
			}
		}
	}
}

object HtmlDispatcher : CoroutineDispatcher(), Delay, DelayFrame {
	private val messageName = "dispatchCoroutine"

	private val queue = object : MessageQueue() {
		override fun schedule() {
			window.postMessage(messageName, "*")
		}
	}

	init {
		window.addEventListener("message", { event: dynamic ->
			if (event.source == window && event.data == messageName) {
				event.stopPropagation()
				queue.process()
			}
		}, true)
	}

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		queue.enqueue(block)
	}

	override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
		window.setTimeout({ with(continuation) { resumeUndispatched(Unit) } }, timeMillis.toInt())
		//window.setTimeout({ with(continuation) { resume(Unit) } }, unit.toMillis(time).toInt())
	}

	override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
		val handle = window.setTimeout({ block.run() }, unit.toMillis(time).toInt())
		return object : DisposableHandle {
			override fun dispose() {
				window.clearTimeout(handle)
			}
		}
	}

	override fun delayFrame(continuation: CancellableContinuation<Unit>) {
		window.requestAnimationFrame { with(continuation) { resumeUndispatched(Unit) } }
		//window.requestAnimationFrame { with(continuation) { resume(Unit) } }
	}

	override fun toString() = "HtmlDispatcher"
}

internal open class Queue<T : Any> {
	private var queue = arrayOfNulls<Any?>(8)
	private var head = 0
	private var tail = 0

	val isEmpty get() = head == tail

	fun poll(): T? {
		if (isEmpty) return null
		val result = queue[head]!!
		queue[head] = null
		head = head.next()
		@Suppress("UNCHECKED_CAST")
		return result as T
	}

	tailrec fun add(element: T) {
		val newTail = tail.next()
		if (newTail == head) {
			resize()
			add(element) // retry with larger size
			return
		}
		queue[tail] = element
		tail = newTail
	}

	private fun resize() {
		var i = head
		var j = 0
		val a = arrayOfNulls<Any?>(queue.size * 2)
		while (i != tail) {
			a[j++] = queue[i]
			i = i.next()
		}
		queue = a
		head = 0
		tail = j
	}

	private fun Int.next(): Int {
		val j = this + 1
		return if (j == queue.size) 0 else j
	}
}

internal abstract class MessageQueue : Queue<Runnable>() {
	val yieldEvery = 16 // yield to JS event loop after this many processed messages

	private var scheduled = false

	abstract fun schedule()

	fun enqueue(element: Runnable) {
		add(element)
		if (!scheduled) {
			scheduled = true
			schedule()
		}
	}

	fun process() {
		try {
			// limit number of processed messages
			repeat(yieldEvery) {
				val element = poll() ?: return@process
				element.run()
			}
		} finally {
			if (isEmpty) {
				scheduled = false
			} else {
				schedule()
			}
		}
	}
}

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) {
	entry(KoruiContext())
}

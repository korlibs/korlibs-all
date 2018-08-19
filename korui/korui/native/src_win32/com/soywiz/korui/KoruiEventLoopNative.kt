package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korui.light.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import com.soywiz.korag.*
import com.soywiz.korui.event.*
import com.soywiz.std.*
import com.soywiz.kds.*
import com.soywiz.kgl.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.reflect.KClass
import com.soywiz.korio.async.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*
import kotlinx.coroutines.experimental.timeunit.*

class MyNativeCoroutineDispatcher() : CoroutineDispatcher(), Delay, Closeable {
	override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

	class TimedTask(val ms: Long, val continuation: CancellableContinuation<Unit>)

	val tasks = Queue<Runnable>()
	val timedTasks = PriorityQueue<TimedTask>(Comparator<TimedTask> { a, b -> a.ms.compareTo(b.ms) })

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		tasks.enqueue(block)
	}

	override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>): Unit {
		val task = TimedTask(Klock.currentTimeMillis() + when (unit) {
			TimeUnit.SECONDS -> time * 1000
			TimeUnit.MILLISECONDS -> time
			else -> error("Unsupported unit $unit")
		}, continuation)
		continuation.invokeOnCancellation {
			timedTasks.remove(task)
		}
		timedTasks.add(task)
	}

	fun executeStep() {
		val now = Klock.currentTimeMillis()
		while (timedTasks.isNotEmpty() && now >= timedTasks.peek().ms) {
			timedTasks.removeHead().continuation.resume(Unit)
		}

		while (tasks.isNotEmpty()) {
			val task = tasks.dequeue()
			task.run()
		}
	}

	override fun close() {

	}

	override fun toString(): String = "MyNativeCoroutineDispatcher"
}

@ThreadLocal
val myNativeCoroutineDispatcher: MyNativeCoroutineDispatcher = MyNativeCoroutineDispatcher()

actual val KoruiDispatcher: CoroutineDispatcher get() = myNativeCoroutineDispatcher

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) {
	entry(KoruiContext())
}


/*
actual object KoruiEventLoop {
	actual val instance: EventLoop by lazy { SdlEventLoop() }
}

class SdlEventLoop : BaseEventLoopNative() {
	override fun start() {

	}

	override fun nativeSleep(time: Int) {
		KorioNative.Thread_sleep(time.toLong())
	}
}
*/

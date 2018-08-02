package com.soywiz.korge.time

import com.soywiz.klock.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.experimental.*
import kotlin.collections.arrayListOf
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign
import kotlin.coroutines.experimental.*

class TimerComponents(override val view: View) : UpdateComponent {
	internal val timers = arrayListOf<(Int) -> Unit>()
	private val timersIt = arrayListOf<(Int) -> Unit>()

	override fun update(ms: Double) {
		timersIt.clear()
		timersIt.addAll(timers)
		for (timer in timersIt) timer(ms.toInt())
	}

	suspend fun wait(time: TimeSpan) = waitMilliseconds(time.ms)

	suspend fun waitFrame() = waitMilliseconds(0)

	suspend fun waitMilliseconds(time: Int): Unit = suspendCancellableCoroutine<Unit> { c ->
		var timer: ((Int) -> Unit)? = null
		var elapsedTime = 0
		timer = {
			elapsedTime += it
			if (elapsedTime >= time) {
				timers -= timer!!
				c.resume(Unit)
			}
		}
		timers += timer
	}
}

class TimerComponent(override val view: View, val totalMs: Double, val callback: () -> Unit) : UpdateComponent, Closeable {
	var elapsed = 0.0
	override fun update(ms: Double) {
		elapsed += ms
		if (elapsed >= totalMs) {
			detach()
		}
	}

	override fun close() {
		detach()
	}
}

val View.timers get() = this.getOrCreateComponent { TimerComponents(this) }
suspend fun View.waitMs(time: Int) = this.timers.waitMilliseconds(time)
suspend fun View.wait(time: TimeSpan) = this.timers.wait(time)
suspend fun View.waitFrame() = this.timers.waitFrame()

suspend fun View.sleepMs(time: Int) = this.timers.waitMilliseconds(time)
suspend fun View.sleep(time: TimeSpan) = this.timers.wait(time)
suspend fun View.sleepFrame() = this.timers.waitFrame()

suspend fun View.delay(time: TimeSpan) = this.timers.wait(time)

fun View.timer(time: TimeSpan, callback: () -> Unit): Closeable = TimerComponent(this, time.milliseconds.toDouble(), callback).attach()

package com.soywiz.korui

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import kotlinx.coroutines.experimental.*
import java.awt.event.*
import java.util.concurrent.*
import javax.swing.*
import kotlin.coroutines.experimental.*

actual val KoruiDispatcher: CoroutineDispatcher get() = Swing

object Swing : CoroutineDispatcher(), Delay, DelayFrame {
	override fun dispatch(context: CoroutineContext, block: Runnable) = SwingUtilities.invokeLater(block)

	override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
		val timer = schedule(time, unit, ActionListener {
			with(continuation) { resumeUndispatched(Unit) }
		})
		continuation.invokeOnCancellation { timer.stop() }
	}

	override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
		val timer = schedule(time, unit, ActionListener {
			block.run()
		})
		return object : DisposableHandle {
			override fun dispose() {
				timer.stop()
			}
		}
	}

	private fun schedule(time: Long, unit: TimeUnit, action: ActionListener): Timer =
		Timer(unit.toMillis(time).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), action).apply {
			isRepeats = false
			start()
		}

	var lastFrameTime = Klock.currentTimeMillis()

	override fun delayFrame(continuation: CancellableContinuation<Unit>) {
		val startFrameTime = Klock.currentTimeMillis()
		val time = (16 - (startFrameTime - lastFrameTime)).clamp(0, 16)
		schedule(time, TimeUnit.MILLISECONDS, ActionListener { continuation.resume(Unit) })
		lastFrameTime = startFrameTime
	}

	override fun toString() = "Swing"
}

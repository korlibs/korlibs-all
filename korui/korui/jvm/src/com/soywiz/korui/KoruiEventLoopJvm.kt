package com.soywiz.korui

import com.soywiz.korui.light.*
import kotlinx.coroutines.experimental.*
import java.awt.event.*
import java.util.concurrent.*
import javax.swing.*
import kotlin.coroutines.experimental.*

actual val KoruiDispatcher: CoroutineDispatcher get() = Swing

object Swing : CoroutineDispatcher(), Delay {
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

	override fun toString() = "Swing"
}

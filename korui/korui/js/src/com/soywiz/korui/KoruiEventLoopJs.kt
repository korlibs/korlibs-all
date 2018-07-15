package com.soywiz.korui

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.timeunit.*
import kotlin.browser.*
import kotlin.coroutines.experimental.*

actual val KoruiDispatcher: CoroutineDispatcher get() = HtmlDispatcher

object HtmlDispatcher : CoroutineDispatcher(), Delay {
	override fun dispatch(context: CoroutineContext, block: Runnable) {
		window.setTimeout({
			block.run()
		}, 0)
	}

	override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
		val timeout = window.setTimeout({
			continuation.resume(Unit)
		}, unit.toMillis(time).toInt())

		continuation.invokeOnCancellation {
			window.clearTimeout(timeout)
		}
	}

	override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
		val timeout = window.setTimeout({
			block.run()
		}, unit.toMillis(time).toInt())

		return object : DisposableHandle {
			override fun dispose() {
				window.clearTimeout(timeout)
			}
		}
	}

	override fun toString() = "HtmlDispatcher"
}

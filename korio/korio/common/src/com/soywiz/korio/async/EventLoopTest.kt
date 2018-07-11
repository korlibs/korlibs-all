package com.soywiz.korio.async

import com.soywiz.korio.coroutine.*

class EventLoopFactoryTest : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopTest()
}

class EventLoopTest(val runOnThread: Long = currentThreadId) : EventLoop(captureCloseables = true) {
	var _time: Long = 0L; private set

	override fun getTime(): Double = _time.toDouble()

	override fun step(ms: Int) {
		_time += ms
		step()
	}

	override fun mustBreak(start: Double, now: Double): Boolean = false
}
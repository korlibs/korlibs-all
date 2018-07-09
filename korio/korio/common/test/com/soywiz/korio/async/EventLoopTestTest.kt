package com.soywiz.korio.async

import kotlin.test.*

class EventLoopTestTest {
	@Test
	fun testDemo() {
		var time = 0
		val el = object : EventLoop() {
			override fun getTime() = time.toDouble()
		}
		fun step(delta: Int) {
			time += delta
			el.step()
		}
		var out = ""
		el.setImmediateDeferred { out += "a" }
		el.setImmediate { out += "0" }
		el.setTimeout(1) { out += "1" }
		el.setTimeout(9) { out += "9" }
		el.setTimeout(5) { out += "5" }
		assertEquals("0", out)
		step(0)
		assertEquals("0a", out)
		step(2)
		assertEquals("0a1", out)
		step(20)
		assertEquals("0a159", out)
	}
}
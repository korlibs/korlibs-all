package com.soywiz.korio.async

import kotlin.test.*

class SleepTest {
	@Test
	fun name() = suspendTest {
		val start = _time
		sleep(10)
		sleep(20)
		val end = _time
		assertTrue((end - start) > 25L)
	}
}
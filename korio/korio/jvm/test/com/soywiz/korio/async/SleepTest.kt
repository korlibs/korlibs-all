package com.soywiz.korio.async

import kotlin.test.*

class SleepTest {
	@Test
	fun name() = suspendTest {
		val start = time
		delay(10)
		delay(20)
		val end = time
		assertTrue((end - start) > 25L)
	}
}
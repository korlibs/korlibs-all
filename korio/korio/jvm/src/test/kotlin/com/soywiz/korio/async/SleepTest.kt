package com.soywiz.korio.async

import kotlin.test.*

class SleepTest {
	@Test
	fun name() = suspendTest {
		val start = time
		sleep(10)
		sleep(20)
		val end = time
		assertTrue((end - start) > 25L)
	}
}
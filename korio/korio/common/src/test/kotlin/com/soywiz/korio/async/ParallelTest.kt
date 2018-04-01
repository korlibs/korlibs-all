package com.soywiz.korio.async

import kotlin.test.*

class ParallelTest {
	@kotlin.test.Test
	fun empty() = suspendTest {
		val out = ""
		parallel()
		assertEquals("", out)
	}

	@kotlin.test.Test
	fun one() = suspendTest {
		var out = ""
		parallel(
			{ sleep(100); out += "a" }
		)
		assertEquals("a", out)
	}

	@kotlin.test.Test
	fun couple() = suspendTest {
		var out = ""
		parallel(
			{ sleep(100); out += "a" },
			{ sleep(200); out += "b" }
		)
		assertEquals("ab", out)
	}
}
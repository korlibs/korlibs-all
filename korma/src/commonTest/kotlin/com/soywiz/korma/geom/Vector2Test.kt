package com.soywiz.korma.geom

import com.soywiz.korma.*
import kotlin.math.*
import kotlin.test.*

class Vector2Test {
	@Test
	fun name() {
		val v = Vector2(1, 1.0)
		//assertEquals(sqrt(2.0), v.length, 0.001)
		assertEquals(sqrt(2.0), v.length)
	}

	@Test
	fun testString() {
		assertEquals("(1, 2)", Vector2(1, 2).toString())

	}
}
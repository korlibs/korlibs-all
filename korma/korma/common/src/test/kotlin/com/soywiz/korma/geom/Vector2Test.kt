package com.soywiz.korma.geom

import com.soywiz.korma.Vector2
import org.junit.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals

class Vector2Test {
	@Test
	fun name() {
		val v = Vector2(1, 1.0)
		//assertEquals(sqrt(2.0), v.length, 0.001)
		assertEquals(sqrt(2.0), v.length)
	}
}
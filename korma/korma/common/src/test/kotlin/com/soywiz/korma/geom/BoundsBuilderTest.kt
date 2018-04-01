package com.soywiz.korma.geom

import kotlin.test.*

class BoundsBuilderTest {
	@Test
	fun name() {
		val bb = BoundsBuilder()
		bb.add(Rectangle(20, 10, 200, 300))
		bb.add(Rectangle(2000, 70, 400, 50))
		bb.add(Rectangle(10000, 10000, 0, 0))
		assertEquals("Rectangle(x=20, y=10, width=2380, height=300)", bb.getBounds().toString())
	}
}
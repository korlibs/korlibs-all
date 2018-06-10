package com.soywiz.kds

import kotlin.test.*

class WeakPropertyTest {
	class C

	var C.prop by WeakProperty { 0 }

	@Test
	fun name() {
		val c1 = C()
		val c2 = C()
		assertEquals(0, c1.prop)
		assertEquals(0, c2.prop)
		c1.prop = 1
		c2.prop = 2
		assertEquals(1, c1.prop)
		assertEquals(2, c2.prop)
	}
}
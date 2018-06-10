package com.soywiz.kds.specialized

import com.soywiz.kds.*
import kotlin.test.*

class IntArrayListTest {
	@Test
	fun name() {
		val values = IntArrayList(2)
		assertEquals(0, values.length)
		assertEquals(2, values.capacity)
		values.add(1)
		assertEquals(listOf(1), values.toList())
		assertEquals(1, values.length)
		assertEquals(2, values.capacity)
		values.add(2)
		assertEquals(listOf(1, 2), values.toList())
		assertEquals(2, values.length)
		assertEquals(2, values.capacity)
		values.add(3)
		assertEquals(listOf(1, 2, 3), values.toList())
		assertEquals(3, values.length)
		assertEquals(6, values.capacity)
	}

	@Test
	fun name2() {
		val v = IntArrayList()
		v.add(1)
		v.add(2)
		v.add(3)
		assertEquals(listOf(1, 2, 3), v.toList())
		v.removeAt(1)
		assertEquals(listOf(1, 3), v.toList())
		assertEquals(2, v.size)
		v.removeAt(1)
		assertEquals(listOf(1), v.toList())
		v.removeAt(0)
		assertEquals(listOf(), v.toList())
	}
}
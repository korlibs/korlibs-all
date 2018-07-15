package com.soywiz.korma.random

import kotlin.test.*

class RandomExtKtTest {
	//class TestRandom(val values: List<Int>) : Random() {
	//	var index = 0
	//	//override fun next(bits: Int): Int = values.getCyclic(index++) and ((1L shl bits) - 1).toInt()
	//	override fun next(bits: Int): Int {
	//		return values.getCyclic(index++)
	//	}
	//}

	@Test
	fun weighted() {
		val random = Rand(0L)
		val weighted = mapOf("a" to 1, "b" to 1)
		random.nextInt()
		assertEquals(
			listOf("b", "a", "a", "a"),
			listOf(random[weighted], random[weighted], random[weighted], random[weighted])
		)
	}

	@Test
	fun weighted2() {
		val weighted = mapOf("a" to 1, "b" to 2)
		val random = Rand(0L)
		assertEquals(
			listOf("b", "a", "b", "b", "b"),
			listOf(random[weighted], random[weighted], random[weighted], random[weighted], random[weighted])
		)
	}
}
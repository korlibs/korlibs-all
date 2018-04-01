package com.soywiz.kzlib

import kotlin.test.*

class KzlibTest {
	@kotlin.test.Test
	fun testDeflateInflate() {
		val original = "HELLO HELLO HELLO HELLO WORLD".toSimpleByteArray()
		val compressed = original.deflate()
		val uncompressed = compressed.inflate()

		assertEquals(original.toList(), uncompressed.toList())
	}
}
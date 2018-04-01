package com.soywiz.kzlib

import com.soywiz.kzlib.simple.*
import kotlin.test.*

class KzlibTest {
	@kotlin.test.Test
	fun testDeflateInflate() {
		val original = "HELLO HELLO HELLO HELLO WORLD".toSimpleByteArray()
		val compressed = original.deflate()
		val uncompressed = compressed.inflate()

		assertEquals(original.toList(), uncompressed.toList())

		val uncompressed2 = SimpleInflater.inflateZlib(compressed)
		assertEquals(original.toList(), uncompressed2.toList())
	}
}
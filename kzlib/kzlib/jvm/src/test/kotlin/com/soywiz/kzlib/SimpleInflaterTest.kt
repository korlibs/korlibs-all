package com.soywiz.kzlib

import com.soywiz.korio.compression.*
import org.junit.Test
import kotlin.test.*

class SimpleInflaterTest {
	@Test
	fun name() {
		val data = "1F8B0808B12CC15A000368656C6C6F2E74787400F370F5F1F157F00093E1FE413E2E8A004633AED812000000".unhex
		val out = SimpleInflater.inflateGzip(data)
		assertEquals("HELLO HELLO WORLD!", out.toASCIIString())
	}
}

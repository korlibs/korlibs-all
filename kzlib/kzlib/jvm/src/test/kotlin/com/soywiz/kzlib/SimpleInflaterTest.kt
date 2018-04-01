package com.soywiz.kzlib

import com.soywiz.kzlib.simple.*
import org.junit.Test
import kotlin.test.*

class SimpleInflaterTest {
	@Test
	fun gzipInflate() {
		val data = "1F8B0808B12CC15A000368656C6C6F2E74787400F370F5F1F157F00093E1FE413E2E8A004633AED812000000".unhex
		val out = SimpleInflater.inflateGzip(data)
		assertEquals("HELLO HELLO WORLD!", out.toASCIIString())
	}
	
	@Test
	fun zlibInflate() {
		val data = "789CF370F5F1F157F0C020C3FD837C5C0074AA07D9".unhex
		val out = SimpleInflater.inflateZlib(data)
		assertEquals("HELLO HELLO HELLO HELLO WORLD", out.toASCIIString())
	}
}

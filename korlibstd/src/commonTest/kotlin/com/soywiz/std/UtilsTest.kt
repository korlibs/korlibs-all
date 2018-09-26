package com.soywiz.std

import kotlinx.atomicfu.*
import kotlin.test.*

object Sample {
	//private val _demo = NewAtomicReference<Map<String, String>>(mapOf())
	//var demo: Map<String, String>
	//	set(value) = run { _demo.set(value) }
	//	get() = _demo.get()!!
	var demo by atomic(mapOf("hello" to "world"))
}

class UtilsTest {
	@Test
	@Ignore // Ignore until figure out how to do it in kotlin-native 0.8
	fun name() {
		Sample.demo = mapOf("hi" to "there")
		assertEquals("there", Sample.demo["hi"])

		Sample.demo += mapOf("meaning-of-life" to "42")
		assertEquals("42", Sample.demo["meaning-of-life"])
	}
}

package com.soywiz.korio.serialization.xml

import com.soywiz.korio.async.*
import com.soywiz.korio.vfs.*
import org.junit.Test
import kotlin.test.*

class Xml2Test {
	@Test
	fun name2() = syncTest {
		val xml = ResourcesVfs["test.xml"].readXml()
		assertEquals("test", xml.name)
		assertEquals("hello", xml.text)
	}
}
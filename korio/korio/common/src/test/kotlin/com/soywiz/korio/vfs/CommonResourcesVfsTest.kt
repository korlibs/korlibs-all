package com.soywiz.korio.vfs

import com.soywiz.korio.KorioNative.syncTest
import org.junit.*

class CommonResourcesVfsTest {
	@Test
	fun testCanReadResourceProperly() = suspendTest {
		//assertEquals("HELLO", resourcesVfs["resource.txt"].readString())
	}
}
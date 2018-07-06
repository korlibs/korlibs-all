package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import kotlin.test.*

class PSDTest {
	val formats = ImageFormats(StandardImageFormats + PSD)
	val ResourcesVfs = ImageFormatsTest.root

	@Test
	fun psdTest() = suspendTest {
		val output = ResourcesVfs["small.psd"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["small.psd.png"].readBitmapNoNative(formats)
		//showImageAndWait(output)
		assertTrue(output.matchContents(expected))
	}
}
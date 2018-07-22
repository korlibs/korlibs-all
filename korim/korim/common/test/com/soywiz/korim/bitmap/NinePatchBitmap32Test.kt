package com.soywiz.korim.bitmap

import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class NinePatchBitmap32Test {
	@Test
	fun name() = suspendTest {
		val ninePatch = ResourcesVfs["bubble-chat.9.png"].readNinePatch(defaultImageFormats)
		assertEquals(listOf(false to 91, true to 67, false to 44), ninePatch.hsegments)
		assertEquals(listOf(false to 57, true to 52, false to 93), ninePatch.vsegments)
	}
}
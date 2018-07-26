package com.soywiz.korge.view

import com.soywiz.korag.log.*
import com.soywiz.korge.render.*
import kotlin.test.*

class TextTest {
	@Test
	fun testRender() {
		val text = Text()
		val ag = LogAG()
		text.render(RenderContext(ag))
	}
}
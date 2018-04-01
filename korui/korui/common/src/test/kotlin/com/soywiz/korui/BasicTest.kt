package com.soywiz.korui

import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import com.soywiz.korio.util.OS
import com.soywiz.korui.light.log.LogLightComponents
import com.soywiz.korui.ui.button
import org.junit.Test
import kotlin.test.assertEquals

class BasicTest {
	val eventLoop = EventLoopTest()
	val lc = LogLightComponents()

	@Test
	fun name() {
		if (OS.isJs) return
		sync(eventLoop) {
			val frame = Application(lc).frame("Title") {
				button("Hello")
			}
			eventLoop.step(60)
			eventLoop.step(60)
			eventLoop.step(60)
			eventLoop.step(60)

			assertEquals(
					"""
				create(FRAME)=0
				setProperty(0,LightProperty[TEXT],Title)
				setBounds(0,0,0,640,480)
				create(BUTTON)=1
				setProperty(1,LightProperty[TEXT],Hello)
				setParent(1,0)
				setBounds(0,0,0,640,480)
				setProperty(0,LightProperty[VISIBLE],true)
				setBounds(1,0,0,640,480)
				setBounds(0,0,0,640,480)
			""".trimIndent(),
					lc.log.joinToString("\n")
			)
		}
	}
}
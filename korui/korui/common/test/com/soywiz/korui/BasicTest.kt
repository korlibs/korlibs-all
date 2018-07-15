package com.soywiz.korui

import com.soywiz.korio.*
import com.soywiz.korui.light.log.*
import com.soywiz.korui.ui.*
import kotlinx.coroutines.experimental.*
import kotlin.test.*

class BasicTest {
	fun applicationTest(callback: suspend Application.(LogLightComponents) -> Unit) {
		val lc = LogLightComponents()
		Korio {
			Application(lc) {
				callback(lc)
			}
		}
	}

	@Test
	fun name() = applicationTest { lc ->
		val frame = frame("Title") {
			button("Hello")
		}

		delay(20)

		assertEquals(
			"""
			create(FRAME)=0
			setProperty(0,LightProperty[TEXT],Title)
			setBounds(0,0,0,640,480)
			setBounds(0,0,0,640,480)
			create(BUTTON)=1
			setProperty(1,LightProperty[TEXT],Hello)
			setParent(1,0)
			setProperty(0,LightProperty[VISIBLE],true)
			setBounds(1,0,0,640,480)
			setBounds(0,0,0,640,480)
		""".trimIndent(),
			lc.log.joinToString("\n")
		)
	}
}
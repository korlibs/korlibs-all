package com.soywiz.korui

import com.soywiz.korio.util.*
import com.soywiz.korui.light.log.*
import com.soywiz.korui.ui.*
import kotlinx.coroutines.experimental.*
import kotlin.test.*

class BasicTest {
	fun applicationTest(callback: suspend Application.(LogLightComponents) -> Unit) {
		val lc = LogLightComponents()
		Korui {
			Application(lc) {
				callback(lc)
			}
		}
	}

	@Test
	fun name(): Unit {
		if (OS.isNative) return // @TODO: Ignore kotlin-native for now
		return applicationTest { lc ->
			val frame = frame("Title") {
				button("Hello")
			}

			delay(20)

			assertEquals(
				"""
					create(FRAME)=0
					setProperty(0,LightProperty[TEXT],Title)
					setBounds(0,0,0,640,480)
					create(BUTTON)=1
					setProperty(1,LightProperty[TEXT],Hello)
					setParent(1,0)
					setBounds(1,0,0,640,480)
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
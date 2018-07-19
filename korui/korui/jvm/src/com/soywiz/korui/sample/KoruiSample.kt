package com.soywiz.korui.sample

import com.soywiz.korui.*
import com.soywiz.korui.ui.*

object KoruiSample {
	@JvmStatic
	fun main(args: Array<String>) = Korui {
		val app = Application()
		app.apply {
			frame("HELLO") {
				vertical {
					button("HI!")
					comboBox(1, 2, 3)
				}
			}
		}
	}
}
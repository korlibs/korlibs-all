package com.soywiz.korag.software

import com.soywiz.korag.*

class AGFactorySoftware() : AGFactory {
	override val supportsNativeFrame: Boolean = false
	override fun create(): AG = AGSoftware()
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}

class AGSoftware : AG() {
	override val nativeComponent: Any = Any()

	init {
		ready()
	}
}
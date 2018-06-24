package com.soywiz.korag

actual object AGFactoryFactory {
	actual fun create(): AGFactory = TODO()
	actual val isTouchDevice: Boolean get() = TODO()
}

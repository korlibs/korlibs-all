package com.soywiz.korag

actual object AGOpenglFactory {
	actual fun create(nativeComponent: Any?): AGFactory = AGFactoryAwt
	actual val isTouchDevice: Boolean = false
}

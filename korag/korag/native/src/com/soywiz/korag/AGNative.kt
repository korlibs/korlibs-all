package com.soywiz.korag

import com.soywiz.korag.software.*

actual object AGFactoryFactory {
	actual fun create(): AGFactory = NativeAGFactory()
	actual val isTouchDevice: Boolean get() = true
}

class NativeAGFactory : AGFactorySoftware() {
}

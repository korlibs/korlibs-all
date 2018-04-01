package com.soywiz.korau.sound

import com.soywiz.korau.*
import com.soywiz.korio.vfs.*

actual object NativeNativeSoundProvider {
	actual val instance: NativeSoundProvider by lazy { HtmlNativeSoundProvider() }
}

actual fun registerNativeSoundSpecialReader(): Unit {
	HtmlNativeSoundSpecialReader().register()
}
package com.soywiz.korau.sound

import com.soywiz.korau.HtmlNativeSoundProvider
import com.soywiz.korau.HtmlNativeSoundSpecialReader
import com.soywiz.korio.vfs.register

actual object NativeNativeSoundProvider {
	actual val instance: NativeSoundProvider by lazy { HtmlNativeSoundProvider() }
}

actual fun registerNativeSoundSpecialReader(): Unit {
	HtmlNativeSoundSpecialReader().register()
}
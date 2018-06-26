package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korui.light.*

actual object KoruiEventLoop {
	actual val instance: EventLoop by lazy { SdlEventLoop() }
}

class SdlEventLoop : BaseEventLoopNative() {
	override fun start() {

	}

	override fun nativeSleep(time: Int) {
		KorioNative.Thread_sleep(time.toLong())
	}
}
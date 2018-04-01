package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korui.light.*

actual object KoruiEventLoop {
	actual val instance: EventLoop by lazy { EventLoopAwt() }
}
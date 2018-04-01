package com.soywiz.korui

import com.soywiz.korio.async.EventLoop
import com.soywiz.korui.light.EventLoopAwt

actual object KoruiEventLoop {
	actual val instance: EventLoop by lazy { EventLoopAwt() }
}
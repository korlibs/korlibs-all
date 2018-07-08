package com.soywiz.korui

import com.soywiz.korio.async.*

actual object KoruiEventLoop {
	actual fun create(): EventLoop = eventLoopFactoryDefaultImpl.createEventLoop()
}

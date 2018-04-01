package com.soywiz.korui

import com.soywiz.korio.async.*

actual object KoruiEventLoop {
	actual val instance: EventLoop by lazy { eventLoopFactoryDefaultImpl.createEventLoop() }
}

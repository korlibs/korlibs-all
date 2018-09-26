package com.soywiz.korui

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.eventLoopFactoryDefaultImpl

actual object KoruiEventLoop {
	actual val instance: EventLoop by lazy { eventLoopFactoryDefaultImpl.createEventLoop() }
}

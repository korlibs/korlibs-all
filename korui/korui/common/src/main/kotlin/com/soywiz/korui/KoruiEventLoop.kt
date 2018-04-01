package com.soywiz.korui

import com.soywiz.korio.async.EventLoop

expect object KoruiEventLoop {
	val instance: EventLoop
}
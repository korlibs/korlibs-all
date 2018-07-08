package com.soywiz.korui

import com.soywiz.korio.async.*

expect object KoruiEventLoop {
	fun create(): EventLoop
}
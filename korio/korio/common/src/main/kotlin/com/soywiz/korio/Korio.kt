package com.soywiz.korio

import com.soywiz.korio.async.*

fun Korio(entry: suspend EventLoop.() -> Unit) = EventLoop.main(entry)

object Korio {
	val VERSION = KORIO_VERSION
}

package com.soywiz.korio.lang

import com.soywiz.korio.*

object GC {
	fun collect() {
		KorioNative.gc()
	}
}
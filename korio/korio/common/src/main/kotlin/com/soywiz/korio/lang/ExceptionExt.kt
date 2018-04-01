package com.soywiz.korio.lang

import com.soywiz.korio.*

fun Throwable.printStackTrace() {
	KorioNative.printStackTrace(this)
}
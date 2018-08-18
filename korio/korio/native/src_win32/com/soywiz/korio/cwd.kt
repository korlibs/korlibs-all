package com.soywiz.korio

import kotlinx.cinterop.*
import platform.posix.*

fun nativeCwd(): String = memScoped {
	val data = allocArray<ByteVar>(1024)
	getcwd(data, 1024)
	data.toKString()
}

fun doMkdir(path: String, attr: Int): Int {
	return platform.posix.mkdir(path)
}

fun realpath(path: String): String = path

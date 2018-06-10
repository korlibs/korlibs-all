package com.soywiz.korio.async

import com.soywiz.korio.*

suspend fun <T> executeInWorker(callback: suspend () -> T): T = KorioNative.executeInWorker(callback)

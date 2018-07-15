package com.soywiz.korio

import com.soywiz.korio.async.*
import kotlinx.coroutines.experimental.*

fun Korio(entry: suspend CoroutineDispatcher.() -> Unit) = Korio(com.soywiz.korio.async.KorioDefaultDispatcher, entry)

fun <T : CoroutineDispatcher> Korio(context: T, entry: suspend T.() -> Unit): Unit {
	KorioNative.asyncEntryPoint(context) { entry(context) }
}

object Korio {
	val VERSION = KORIO_VERSION
}

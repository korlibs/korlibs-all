package com.soywiz.korui

import com.soywiz.korio.*
import kotlinx.coroutines.experimental.*

expect val KoruiDispatcher: CoroutineDispatcher

open class KoruiContext

fun Korui(context: CoroutineDispatcher = KoruiDispatcher, entry: suspend (KoruiContext) -> Unit) {
	Korio(context) { KoruiWrap { entry(it) } }
}

internal expect suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit)

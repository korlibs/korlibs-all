package com.soywiz.korui

import com.soywiz.korio.*
import kotlinx.coroutines.experimental.*

expect val KoruiDispatcher: CoroutineDispatcher

fun Korui(context: CoroutineDispatcher = KoruiDispatcher, entry: suspend () -> Unit): Unit {
	Korio(context) { entry() }
}

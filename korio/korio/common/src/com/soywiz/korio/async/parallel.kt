package com.soywiz.korio.async

import kotlinx.coroutines.experimental.*

suspend fun parallel(vararg callbacks: suspend () -> Unit) {
	val jobs = callbacks.map { launch(KorioDefaultDispatcher) { it() } }
	for (job in jobs) job.join()
}


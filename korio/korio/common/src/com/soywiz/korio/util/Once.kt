package com.soywiz.korio.util

import kotlinx.coroutines.experimental.*

class Once {
	var completed = false

	inline operator fun invoke(callback: () -> Unit) {
		if (!completed) {
			completed = true
			callback()
		}
	}
}

class AsyncOnce<T> {
	var promise: Deferred<T>? = null

	suspend operator fun invoke(callback: suspend () -> T): T {
		if (promise == null) {
			promise = async { callback() }
		}
		return promise!!.await()
	}
}

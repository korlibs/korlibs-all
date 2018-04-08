package com.soywiz.korio.util

import com.soywiz.korio.async.*

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
	var promise: Promise<T>? = null

	suspend operator fun invoke(callback: suspend () -> T): T {
		if (promise == null) {
			promise = async { callback() }
		}
		return promise!!.await()
	}
}
package com.soywiz.korte

import com.soywiz.korio.async.await
import kotlin.test.assertEquals

open class BaseTest {
	suspend inline fun <reified T> expectException(message: String, noinline callback: suspend () -> Unit) {
		try {
			callback.await()
		} catch (e: Throwable) {
			if (e is T) {
				assertEquals(message, e.message)
			} else {
				throw e
			}
		}
	}
}
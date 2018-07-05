package com.soywiz.klock

actual class AtomicReference<T>(dummy: Boolean) {
	var value: T? = null
}

actual fun <T> NewAtomicReference(): AtomicReference<T> = AtomicReference(true)
actual fun <T> AtomicReference<T>.get(): T? = this.value
actual fun <T> AtomicReference<T>.set(value: T?) = run { this.value = value }

actual annotation class ThreadLocal

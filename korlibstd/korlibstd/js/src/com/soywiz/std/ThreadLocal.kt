package com.soywiz.std

actual annotation class ThreadLocal actual constructor()

// Temporal From: https://github.com/JetBrains/kotlin-native/pull/1769
actual fun <T> atomicLazy(initializer: () -> T): Lazy<T> = lazy(initializer)

actual class AtomicReference<T>(var value: T) {
}

actual fun <T> NewAtomicReference(value: T): AtomicReference<T> = AtomicReference<T>(value)

actual fun <T> AtomicReference<T>.set(value: T) {
	this.value = value
}

actual fun <T> AtomicReference<T>.get(): T {
	return this.value
}

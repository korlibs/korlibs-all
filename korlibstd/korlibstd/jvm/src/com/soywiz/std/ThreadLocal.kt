package com.soywiz.std

actual annotation class ThreadLocal actual constructor()

// Temporal From: https://github.com/JetBrains/kotlin-native/pull/1769
actual fun <T> atomicLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.SYNCHRONIZED, initializer)

actual typealias AtomicReference<T> = java.util.concurrent.atomic.AtomicReference<T>

actual fun <T> NewAtomicReference(value: T): AtomicReference<T> = java.util.concurrent.atomic.AtomicReference<T>(value)


actual fun <T> AtomicReference<T>.set(value: T) {
	(this as java.util.concurrent.atomic.AtomicReference<T>).set(value)
}
actual fun <T> AtomicReference<T>.get(): T {
	return (this as java.util.concurrent.atomic.AtomicReference<T>).get()
}
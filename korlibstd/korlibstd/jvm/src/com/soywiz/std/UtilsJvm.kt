@file:Suppress("USELESS_CAST")

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

actual typealias AtomicInt = java.util.concurrent.atomic.AtomicInteger

actual fun NewAtomicInt(value: Int): AtomicInt = AtomicInt(value)

actual fun AtomicInt.compareAndSet(expected: Int, newValue: Int): Boolean {
	return (this as java.util.concurrent.atomic.AtomicInteger).compareAndSet(expected, newValue)
}

actual fun AtomicInt.addAndGet(delta: Int): Int {
	return (this as java.util.concurrent.atomic.AtomicInteger).addAndGet(delta)
}

actual fun AtomicInt.set(value: Int) {
	(this as java.util.concurrent.atomic.AtomicInteger).set(value)
}

actual fun AtomicInt.get(): Int {
	return (this as java.util.concurrent.atomic.AtomicInteger).get()
}



actual typealias AtomicLong = java.util.concurrent.atomic.AtomicLong

actual fun NewAtomicLong(value: Long): AtomicLong = AtomicLong(value)

actual fun AtomicLong.addAndGet(delta: Long): Long {
	return (this as java.util.concurrent.atomic.AtomicLong).addAndGet(delta)
}

actual fun AtomicLong.set(value: Long) {
	(this as java.util.concurrent.atomic.AtomicLong).set(value)
}

actual fun AtomicLong.get(): Long {
	return (this as java.util.concurrent.atomic.AtomicLong).get()
}

actual val isNative: Boolean = false
actual val isJs: Boolean = false
actual val isJvm: Boolean = true

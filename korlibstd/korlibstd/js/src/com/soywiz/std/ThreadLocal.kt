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

actual class AtomicInt {
	var value: Int = 0
}

actual fun NewAtomicInt(value: Int): AtomicInt = AtomicInt().apply { this.value = value }
actual fun AtomicInt.addAndGet(delta: Int): Int {
	this.value += delta
	return this.value
}

actual fun AtomicInt.set(value: Int) {
	this.value = value
}

actual fun AtomicInt.get(): Int {
	return this.value
}


actual class AtomicLong {
	var value: Long = 0L
}

actual fun NewAtomicLong(value: Long): AtomicLong = AtomicLong().apply { this.value = value }
actual fun AtomicLong.addAndGet(delta: Long): Long {
	this.value += delta
	return this.value
}

actual fun AtomicLong.set(value: Long) {
	this.value = value
}

actual fun AtomicLong.get(): Long {
	return this.value
}

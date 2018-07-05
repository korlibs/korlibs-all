package com.soywiz.klock

actual typealias AtomicReference<T> = java.util.concurrent.atomic.AtomicReference<T>

actual fun <T> NewAtomicReference() = java.util.concurrent.atomic.AtomicReference<T>()

actual fun <T> AtomicReference<T>.get(): T? {
	return (this as java.util.concurrent.atomic.AtomicReference<T>).get()
}
actual fun <T> AtomicReference<T>.set(value: T?) {
	(this as java.util.concurrent.atomic.AtomicReference<T>).set(value)
}

actual annotation class ThreadLocal

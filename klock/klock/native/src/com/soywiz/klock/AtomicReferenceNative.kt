package com.soywiz.klock

actual typealias AtomicReference<T> = konan.worker.AtomicReference<T>

actual fun <T> NewAtomicReference() = konan.worker.AtomicReference<T>()

actual fun <T> AtomicReference<T>.get(): T? {
	return (this as konan.worker.AtomicReference<T>).get()
}
actual fun <T> AtomicReference<T>.set(value: T?) {
	(this as konan.worker.AtomicReference<T>).set(value)
}

actual typealias ThreadLocal = konan.ThreadLocal

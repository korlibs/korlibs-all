package com.soywiz.std

import konan.*
import konan.worker.*

actual typealias ThreadLocal = konan.ThreadLocal

actual public fun <T> atomicLazy(initializer: () -> T): Lazy<T> = konan.worker.atomicLazy(initializer)

actual typealias AtomicReference<T> = konan.worker.AtomicReference<T>

actual fun <T> NewAtomicReference(value: T): AtomicReference<T> = konan.worker.AtomicReference<T>(value)

actual fun <T> AtomicReference<T>.set(value: T) {
	val fvalue = value.freeze()
	if (this.get() != fvalue) {
		while (this.compareAndSwap(this.get(), fvalue) != fvalue) {
			// spinlock
		}
	}

}

actual fun <T> AtomicReference<T>.get(): T {
	return (this as konan.worker.AtomicReference<T>).get() as T
}

actual typealias AtomicInt = konan.worker.AtomicInt

actual fun NewAtomicInt(value: Int): AtomicInt = konan.worker.AtomicInt(value)

actual fun AtomicInt.addAndGet(delta: Int): Int {
	return (this as konan.worker.AtomicInt).addAndGet(delta)
}

actual fun AtomicInt.set(value: Int) {
	(this as konan.worker.AtomicInt).compareAndSwap(this.get(), value)
}
actual fun AtomicInt.get(): Int {
	return (this as konan.worker.AtomicInt).get()
}




actual typealias AtomicLong = konan.worker.AtomicLong

actual fun NewAtomicLong(value: Long): AtomicLong = konan.worker.AtomicLong(value)

actual fun AtomicLong.addAndGet(delta: Long): Long {
	return (this as konan.worker.AtomicLong).addAndGet(delta)
}

actual fun AtomicLong.set(value: Long) {
	(this as konan.worker.AtomicLong).compareAndSwap(this.get(), value)
}
actual fun AtomicLong.get(): Long {
	return (this as konan.worker.AtomicLong).get()
}
package com.soywiz.std

import konan.*
import konan.worker.*

actual typealias ThreadLocal = konan.ThreadLocal

// Until in Konan. Temporarily From: https://github.com/JetBrains/kotlin-native/pull/1769

internal object UNINITIALIZED
internal object INITIALIZING

//@konan.internal.Frozen
internal class AtomicLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
	private val initializer_ = konan.worker.AtomicReference<Function0<T>?>(initializer.freeze())
	private val value_ = konan.worker.AtomicReference<Any?>(UNINITIALIZED)

	override val value: T
		get() {
			if (value_.compareAndSwap(UNINITIALIZED, INITIALIZING) === UNINITIALIZED) {
				// We execute exclusively here.
				val ctor = initializer_.get()
				if (ctor != null && initializer_.compareAndSwap(ctor, null) === ctor) {
					value_.compareAndSwap(INITIALIZING, ctor().freeze())
				} else {
					// Something wrong.
					assert(false)
				}
			}
			var result: Any?
			do {
				result = value_.get()
			} while (result === INITIALIZING)

			assert(result !== UNINITIALIZED && result != INITIALIZING)
			@Suppress("UNCHECKED_CAST")
			return result as T
		}

	// Racy!
	override fun isInitialized(): Boolean = value_.get() !== UNINITIALIZED

	override fun toString(): String = if (isInitialized())
		value_.get().toString() else "Lazy value not initialized yet."
}

/**
 * Atomic lazy initializer, could be used in frozen objects, freezes initializing lambda,
 * so use very carefully.
 */
actual public fun <T> atomicLazy(initializer: () -> T): Lazy<T> = AtomicLazyImpl(initializer)

actual typealias AtomicReference<T> = konan.worker.AtomicReference<T>

actual fun <T> NewAtomicReference(value: T): AtomicReference<T> = konan.worker.AtomicReference<T>(value)

actual fun AtomicInt.compareAndSet(expected: Int, newValue: Int): Boolean =
	compareAndSwap(expected, newValue) == expected

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
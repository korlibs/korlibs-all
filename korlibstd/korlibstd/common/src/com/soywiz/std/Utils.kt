package com.soywiz.std

import kotlin.reflect.*

expect annotation class ThreadLocal()

// Until in Konan. Temporarily From: https://github.com/JetBrains/kotlin-native/pull/1769
expect fun <T> atomicLazy(initializer: () -> T): Lazy<T>

expect class AtomicReference<T>

expect class AtomicInt

expect fun NewAtomicInt(value: Int): AtomicInt
expect fun AtomicInt.addAndGet(delta: Int): Int
expect fun AtomicInt.set(value: Int)
expect fun AtomicInt.get(): Int

fun AtomicInt.preIncrement() = addAndGet(+1) - 1
fun AtomicInt.increment() = addAndGet(+1)
fun AtomicInt.decrement() = addAndGet(-1)

expect fun <T> NewAtomicReference(value: T): AtomicReference<T>

expect fun <T> AtomicReference<T>.set(value: T)
expect fun <T> AtomicReference<T>.get(): T

class atomicRef<T>(var initial: T) {
	val value = NewAtomicReference<T?>(initial)

	inline operator fun getValue(obj: Any?, property: KProperty<*>): T {
		@Suppress("UNCHECKED_CAST")
		return this.value.get() as T
	}

	inline operator fun setValue(obj: Any?, property: KProperty<*>, v: T) {
		this.value.set(v)
	}
}

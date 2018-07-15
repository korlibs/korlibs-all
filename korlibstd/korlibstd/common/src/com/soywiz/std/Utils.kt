package com.soywiz.std

import kotlin.reflect.*

expect annotation class ThreadLocal()

// Until in Konan. Temporarily From: https://github.com/JetBrains/kotlin-native/pull/1769
expect fun <T> atomicLazy(initializer: () -> T): Lazy<T>

expect class AtomicReference<T>

class AtomicBool(val value: Boolean = false) {
	fun Boolean.toInt() = if (value) 1 else 0

	private val atomic = NewAtomicInt(value.toInt())
	fun set(value: Boolean) = atomic.set(value.toInt())
	fun get(): Boolean = atomic.get() != 0

	inline operator fun getValue(obj: Any?, property: KProperty<*>): Boolean {
		@Suppress("UNCHECKED_CAST")
		return get()
	}

	inline operator fun setValue(obj: Any?, property: KProperty<*>, v: Boolean) {
		set(v)
	}

	fun compareAndSet(expected: Boolean, newValue: Boolean): Boolean {
		return atomic.compareAndSet(expected.toInt(), newValue.toInt())
	}
}

expect class AtomicInt

expect fun NewAtomicInt(value: Int): AtomicInt
expect fun AtomicInt.compareAndSet(expected: Int, newValue: Int): Boolean
expect fun AtomicInt.addAndGet(delta: Int): Int
expect fun AtomicInt.set(value: Int)
expect fun AtomicInt.get(): Int

fun AtomicInt.preIncrement() = addAndGet(+1) - 1
fun AtomicInt.increment() = addAndGet(+1)
fun AtomicInt.decrement() = addAndGet(-1)

expect class AtomicLong

expect fun NewAtomicLong(value: Long): AtomicLong
expect fun AtomicLong.addAndGet(delta: Long): Long
expect fun AtomicLong.set(value: Long)
expect fun AtomicLong.get(): Long

fun AtomicLong.preIncrement() = addAndGet(+1) - 1
fun AtomicLong.increment() = addAndGet(+1)
fun AtomicLong.decrement() = addAndGet(-1)

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

class atomicLateinit<T>() {
	val value = NewAtomicReference<T?>(null)

	operator fun getValue(obj: Any?, property: KProperty<*>): T {
		if (this.value.get() == null) error("Tried to access atomicLateinit value without setting it first")
		@Suppress("UNCHECKED_CAST")
		return this.value.get() as T
	}

	operator fun setValue(obj: Any?, property: KProperty<*>, v: T) {
		this.value.set(v)
	}
}
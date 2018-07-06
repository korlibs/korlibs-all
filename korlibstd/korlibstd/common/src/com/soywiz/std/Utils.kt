package com.soywiz.std

import kotlin.reflect.*

expect annotation class ThreadLocal()

// Until in Konan. Temporarily From: https://github.com/JetBrains/kotlin-native/pull/1769
expect fun <T> atomicLazy(initializer: () -> T): Lazy<T>

expect class AtomicReference<T>

expect fun <T> NewAtomicReference(value: T): AtomicReference<T>

expect fun <T> AtomicReference<T>.set(value: T)
expect fun <T> AtomicReference<T>.get(): T

class atomicRef<T>(var initial: T) {
	val value = NewAtomicReference<T?>(initial)

	inline operator fun getValue(obj: Any, property: KProperty<*>): T {
		@Suppress("UNCHECKED_CAST")
		return this.value.get() as T
	}

	inline operator fun setValue(obj: Any, property: KProperty<*>, v: T) {
		this.value.set(v)
	}
}

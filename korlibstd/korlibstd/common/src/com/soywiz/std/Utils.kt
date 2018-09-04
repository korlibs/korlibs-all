package com.soywiz.std

import kotlinx.atomicfu.*
import kotlin.reflect.*

expect annotation class ThreadLocal()

internal class AtomicLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
	private object UNINITIALIZED
	private object INITIALIZING

	private val initializer_ = atomic<Function0<T>?>(initializer)
	private val value_ = atomic<Any?>(UNINITIALIZED)

	override val value: T
		get() {
			if (value_.compareAndSet(UNINITIALIZED, INITIALIZING)) {
				// We execute exclusively here.
				val ctor = initializer_.value
				if (ctor != null && initializer_.compareAndSet(ctor, null)) {
					value_.compareAndSet(INITIALIZING, ctor())
				} else {
					// Something wrong.
					check(false)
				}
			}
			var result: Any?
			do {
				result = value_.value
			} while (result === INITIALIZING)

			check(result !== UNINITIALIZED && result != INITIALIZING)
			@Suppress("UNCHECKED_CAST")
			return result as T
		}

	// Racy!
	override fun isInitialized(): Boolean = value_.value !== UNINITIALIZED

	override fun toString(): String = if (isInitialized())
		value_.value.toString() else "Lazy value not initialized yet."
}

// Until in Konan. Temporarily From: https://github.com/JetBrains/kotlin-native/pull/1769
fun <T> atomicLazy(initializer: () -> T): Lazy<T> = AtomicLazyImpl(initializer)

class atomicRef<T>(initial: T) {
	val atomic = atomic(initial)

	//operator var value: T
	//	get() = atomic.value
	//	set(value) {
	//		atomic.value = value
	//	}

	operator fun getValue(obj: Any, property: KProperty<Any?>): T = atomic.value
	operator fun setValue(obj: Any, property: KProperty<Any?>, value: T) = run { atomic.value = value }
}

expect val isNative: Boolean
expect val isJs: Boolean
expect val isJvm: Boolean

expect inline fun <R> synchronized2(lock: Any, block: () -> R): R

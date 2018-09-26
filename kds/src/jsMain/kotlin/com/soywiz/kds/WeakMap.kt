package com.soywiz.kds

@JsName("WeakMap")
external class JsWeakMap {
	fun has(k: dynamic): Boolean
	fun set(k: dynamic, v: dynamic): Unit
	fun get(k: dynamic): dynamic
}

actual class WeakMap<K, V> {
	val wm = JsWeakMap()

	actual operator fun contains(key: K): Boolean = wm.has(key)
	actual operator fun set(key: K, value: V) {
		if (key is String) error("Can't use String as WeakMap keys")
		wm.set(key, value)
	}
	actual operator fun get(key: K): V? = wm.get(key).unsafeCast<V?>()
}
package com.soywiz.kds

actual class WeakMap<K, V> {
	val wm = LinkedHashMap<K, V>()
	init {
		println("No WeakMap supported at this point for Native")
	}
	actual operator fun contains(key: K): Boolean = wm.containsKey(key)
	actual operator fun set(key: K, value: V) = run {
		if (key is String) error("Can't use String as WeakMap keys")
		wm[key] = value
	}
	actual operator fun get(key: K): V? = wm[key]
}
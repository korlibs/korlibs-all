package com.soywiz.kds

class CacheMap<K, V>(val maxSize: Int = 16, val free: (K, V) -> Unit = { k, v -> }) {
	val entries = LinkedHashMap<K, V>()

	val size: Int get() = entries.size
	fun has(key: K) = entries.containsKey(key)

	fun remove(key: K) {
		val value = entries.remove(key)
		if (value != null) free(key, value)
	}

	operator fun get(key: K) = entries[key]
	operator fun set(key: K, value: V) {
		if (size >= maxSize && !entries.containsKey(key)) remove(entries.keys.first())

		val oldValue = entries[key]
		if (oldValue != value) {
			remove(key) // refresh if exists
			entries[key] = value
		}
	}

	inline fun getOrPut(key: K, callback: (K) -> V): V {
		if (!has(key)) set(key, callback(key))
		return get(key)!!
	}

	fun clear() {
		val keys = entries.keys.toList()
		for (key in keys) remove(key)
	}
}

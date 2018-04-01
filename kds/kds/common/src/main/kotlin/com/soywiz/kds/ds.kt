package com.soywiz.kds

fun <K, V> lmapOf(vararg pairs: Pair<K, V>): LinkedHashMap<K, V> {
	val out = LinkedHashMap<K, V>()
	for ((key, value) in pairs) out.put(key, value)
	return out
}

fun <K, V> Iterable<Pair<K, V>>.toLinkedMap(): LinkedHashMap<K, V> {
	val out = LinkedHashMap<K, V>()
	for ((key, value) in this) out.put(key, value)
	return out
}

package com.soywiz.kds

fun <T> FastStringMap<T>.values(): List<T> = this.keys().map { this[it] } as List<T>
val <T> FastStringMap<T>.keys: List<String> get() = keys()
val <T> FastStringMap<T>.values: List<T> get() = values()

inline fun <T> FastStringMap<T>.getNull(key: String?): T? = if (key == null) null else get(key)

inline fun <T> FastStringMap<T>.getOrPut(key: String, callback: () -> T): T {
	val res = get(key)
	if (res != null) return res
	val out = callback()
	set(key, out)
	return out
}

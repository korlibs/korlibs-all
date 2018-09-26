package com.soywiz.kds

actual class FastStringMap<T>(dummy: Boolean)
//actual typealias FastStringMap<T> = Any<T>

actual fun <T> FastStringMap(): FastStringMap<T> = js("(new Map())")
actual inline operator fun <T> FastStringMap<T>.get(key: String): T? = (this.asDynamic()).get(key)
actual inline operator fun <T> FastStringMap<T>.set(key: String, value: T): Unit = run { (this.asDynamic()).set(key, value) }
actual inline operator fun <T> FastStringMap<T>.contains(key: String): Boolean = (this.asDynamic()).has(key)
actual inline fun <T> FastStringMap<T>.remove(key: String): Unit = run { (this.asDynamic()).delete(key) }
actual inline fun <T> FastStringMap<T>.clear() = run { (this.asDynamic()).clear() }

//@JsName("delete")
//external fun jsDelete(v: dynamic): Unit
actual fun <T> FastStringMap<T>.keys(): List<String> {
	return Array_from((this.asDynamic()).keys()).unsafeCast<Array<String>>().toList()
}

private fun Array_from(value: dynamic): Array<dynamic> = js("(Array.from(value))")
@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.kds

actual class FastIntMap<T>(dummy: Boolean)
//actual typealias FastIntMap<T> = Any<T>

actual fun <T> FastIntMap(): FastIntMap<T> = js("({})")
actual inline operator fun <T> FastIntMap<T>.get(key: Int): T? = (this.asDynamic())[key]
actual inline operator fun <T> FastIntMap<T>.set(key: Int, value: T): Unit = run { (this.asDynamic())[key] = value }
actual inline operator fun <T> FastIntMap<T>.contains(key: Int): Boolean = (this.asDynamic())[key] != undefined
actual inline fun <T> FastIntMap<T>.remove(key: Int): Unit = run { (this.asDynamic())[key] = null }
actual inline fun <T> FastIntMap<T>.removeRange(src: Int, dst: Int) {
	val obj = this.asDynamic()
	js("for (var key in obj) if (key >= src && key <= dst) delete obj[key];")
}
actual inline fun <T> FastIntMap<T>.clear() {
	val obj = this.asDynamic()
	js("for (var key in obj) delete obj[key];")
}

//@JsName("delete")
//external fun jsDelete(v: dynamic): Unit

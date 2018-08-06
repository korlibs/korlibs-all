package com.soywiz.kds

expect class FastStringMap<T>

expect fun <T> FastStringMap(): FastStringMap<T>
expect fun <T> FastStringMap<T>.keys(): List<String>
expect operator fun <T> FastStringMap<T>.get(key: String): T?
expect operator fun <T> FastStringMap<T>.set(key: String, value: T): Unit
expect operator fun <T> FastStringMap<T>.contains(key: String): Boolean
expect fun <T> FastStringMap<T>.remove(key: String)
expect fun <T> FastStringMap<T>.clear()

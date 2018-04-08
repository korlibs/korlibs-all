package com.soywiz.kds

expect class FastIntMap<T>

expect fun <T> FastIntMap(): FastIntMap<T>
expect operator fun <T> FastIntMap<T>.get(key: Int): T?
expect operator fun <T> FastIntMap<T>.set(key: Int, value: T): Unit
expect operator fun <T> FastIntMap<T>.contains(key: Int): Boolean
expect fun <T> FastIntMap<T>.remove(key: Int)
expect fun <T> FastIntMap<T>.removeRange(src: Int, dst: Int)
expect fun <T> FastIntMap<T>.clear()

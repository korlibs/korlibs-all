package com.soywiz.kds

import kotlin.reflect.*

class WeakProperty<V>(val gen: () -> V) {
	val map = WeakMap<Any, V>()

	operator fun getValue(obj: Any, property: KProperty<*>): V = map.getOrPut(obj) { gen() }
	operator fun setValue(obj: Any, property: KProperty<*>, value: V) = run { map[obj] = value }
}

class WeakPropertyThis<T, V>(val gen: T.() -> V) {
	val map = WeakMap<T, V>()

	operator fun getValue(obj: T, property: KProperty<*>): V = map.getOrPut(obj) { gen(obj) }
	operator fun setValue(obj: T, property: KProperty<*>, value: V) = run { map[obj] = value }
}
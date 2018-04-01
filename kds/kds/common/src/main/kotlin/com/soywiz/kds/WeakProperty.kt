package com.soywiz.kds

import kotlin.reflect.KProperty

class WeakProperty<V>(val gen: () -> V) {
	val map = WeakMap<Any, V>()

	operator fun getValue(obj: Any, property: KProperty<*>): V = map.getOrPut(obj) { gen() }
	operator fun setValue(obj: Any, property: KProperty<*>, value: V) = run { map.set(obj, value) }
}
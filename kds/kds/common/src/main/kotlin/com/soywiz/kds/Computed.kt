package com.soywiz.kds

import kotlin.reflect.*

class Computed<K : Computed.WithParent<K>, T>(val prop: KProperty1<K, T?>, val default: () -> T) {
	interface WithParent<T> {
		val parent: T?
	}

	operator fun getValue(thisRef: K?, p: KProperty<*>): T {
		var current: K? = thisRef
		while (current != null) {
			val result = prop.get(current)
			if (result != null) return result
			current = current.parent
		}

		return default()
	}
}

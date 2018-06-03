package com.soywiz.kds

import kotlin.reflect.*

interface Extra {
	var extra: LinkedHashMap<String, Any?>?

	class Mixin(override var extra: LinkedHashMap<String, Any?>? = null) : Extra

	@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

	class Property<T : Any?>(val name: String? = null, val defaultGen: () -> T) {
		inline operator fun getValue(thisRef: Extra, property: KProperty<*>): T {
			val res = (thisRef.extra?.get(name ?: property.name) as T?)
			if (res == null) {
				val r = defaultGen()
				setValue(thisRef, property, r)
				return r
			}
			return res
		}

		inline operator fun setValue(thisRef: Extra, property: KProperty<*>, value: T): Unit = run {
			//beforeSet(value)
			thisRef.setExtra(name ?: property.name, value as Any?)
			//afterSet(value)
		}
	}

	class PropertyThis<in T2 : Extra, T : Any?>(val name: String? = null, val defaultGen: T2.() -> T) {
		inline operator fun getValue(thisRef: T2, property: KProperty<*>): T {
			val res = (thisRef.extra?.get(name ?: property.name) as T?)
			if (res == null) {
				val r = defaultGen(thisRef)
				setValue(thisRef, property, r)
				return r
			}
			return res
		}

		inline operator fun setValue(thisRef: T2, property: KProperty<*>, value: T): Unit = run {
			//beforeSet(value)
			if (thisRef.extra == null) thisRef.extra = lmapOf()
			thisRef.extra?.set(name ?: property.name, value as Any?)
			//afterSet(value)
		}
	}
}

fun <T : Any> Extra.getExtraTyped(name: String): T? = extra?.get(name) as T?
fun Extra.getExtra(name: String): Any? = extra?.get(name)
fun Extra.setExtra(name: String, value: Any?): Unit {
	if (extra == null) extra = LinkedHashMap()
	extra?.set(name, value)
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
class extraProperty<T : Any?>(val name: String? = null, val default: () -> T) {
	inline operator fun getValue(thisRef: Extra, property: KProperty<*>): T =
		(thisRef.extra?.get(name ?: property.name) as T?) ?: default()

	inline operator fun setValue(thisRef: Extra, property: KProperty<*>, value: T): Unit = run {
		if (thisRef.extra == null) thisRef.extra = lmapOf()
		thisRef.extra?.set(name ?: property.name, value as Any?)
	}
}

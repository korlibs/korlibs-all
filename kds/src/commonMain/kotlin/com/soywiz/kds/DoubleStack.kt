package com.soywiz.kds

class DoubleStack() {
	private val items = DoubleArrayList()

	val size: Int get() = items.size
	val hasMore: Boolean get() = size > 0
	fun isEmpty() = size == 0
	fun isNotEmpty() = size != 0

	constructor(vararg items: Double) : this() {
		for (item in items) push(item)
	}

	fun push(v: Double) {
		items.add(v)
	}

	fun pop(): Double = items.removeAt(items.size - 1)
}

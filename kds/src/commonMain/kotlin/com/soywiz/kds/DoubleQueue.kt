package com.soywiz.kds

class DoubleQueue() {
	private val items = DoubleCircularList()

	val size: Int get() = items.size
	val hasMore: Boolean get() = size > 0
	fun isEmpty() = size == 0
	fun isNotEmpty() = size != 0

	constructor(vararg items: Double) : this() {
		for (item in items) enqueue(item)
	}

	fun enqueue(v: Double) {
		items.addLast(v)
	}

	fun dequeue(): Double = items.removeFirst()
}

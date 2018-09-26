package com.soywiz.kds

class ListReader<T>(val list: List<T>) {
	var position = 0
	val size: Int get() = list.size
	val eof: Boolean get() = position >= list.size
	val hasMore: Boolean get() = position < list.size
	fun peek(): T = list[position]
	fun skip(count: Int = 1) = this.apply { this.position += count }
	fun read(): T = peek().apply { skip(1) }

	fun dump() {
		for (item in list) {
			println(item)
		}
	}
}

fun <T> List<T>.reader() = ListReader(this)

fun <T> ListReader<T>.expect(value: T): T {
	val v = read()
	if (v != value) error("Expecting '$value' but found '$v'")
	return v
}


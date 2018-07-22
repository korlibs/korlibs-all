package com.dragonbones.util

import java.util.*

class Array<T> @JvmOverloads constructor(data: Array<T>, _length: Int = data.size) : ArrayBase<T>() {
	private var data = arrayOfNulls<Any>(0) as Array<T>

	constructor() : this(arrayOfNulls<Any>(16) as Array<T>, 0) {}

	constructor(length: Int) : this(arrayOfNulls<Any>(length) as Array<T>, length) {}

	init {
		this.data = data
	}

	override fun create(count: Int): ArrayBase<T> {
		return Array(count)
	}

	private fun ensureCapacity(minLength: Int) {
		if (data.size < minLength) {
			data = Arrays.copyOf<T>(data, Math.max(minLength, data.size * 3))
		}
	}

	operator fun get(index: Int): T {
		return data[index]
	}

	operator fun set(index: Int, value: T?) {
		data[index] = value
	}

	override fun getObject(index: Int): T {
		return get(index)
	}

	override fun setObject(index: Int, value: T) {
		set(index, value)
	}

	//@Deprecated
	fun add(value: T) {
		pushObject(value)
	}

	fun push(value: T) {
		pushObject(value)
	}

	var length: Int
		get() = _length
		set(value) {
			_length = length
			ensureCapacity(length)
		}

	override fun copy(): Array<T> {
		val out = Array<T>()
		out.length = length
		out.data = Arrays.copyOf<T>(data, data.size)
		return out
	}

	override fun sort(comparator: Comparator<T>, start: Int, end: Int) {
		Arrays.sort(data, start, end, comparator)
	}
}

package com.soywiz.kds

import kotlin.math.*

class FloatArrayList(capacity: Int = 7) : NumberArrayList(), Collection<Float> {
	var data: FloatArray = FloatArray(capacity); private set
	internal val capacity: Int get() = data.size
	private var length: Int = 0
	override var size: Int get() = length
		set(value) {
			ensure(value)
			this.length = value
		}

	constructor(other: FloatArrayList) : this() {
		add(other)
	}

	constructor(vararg other: Float) : this() {
		add(other)
	}

	private fun ensure(count: Int) {
		if (length + count > data.size) {
			data = data.copyOf(max(length + count, data.size * 3))
		}
	}

	fun clear() = run { length = 0 }

	fun add(value: Float) {
		ensure(1)
		data[length++] = value
	}

	operator fun plusAssign(value: Float) = add(value)
	operator fun plusAssign(value: FloatArray) = add(value)
	operator fun plusAssign(value: FloatArrayList) = add(value)
	operator fun plusAssign(value: Iterable<Float>) = add(value)

	fun add(values: FloatArray, offset: Int = 0, length: Int = values.size) {
		ensure(values.size)
		MemTools.arraycopy(values, offset, data, this.length, length)
		this.length += values.size
	}

	fun add(values: FloatArrayList) = add(values.data, 0, values.length)
	fun add(values: Iterable<Float>) = run { for (v in values) add(v) }

	operator fun get(index: Int): Float {
		//if (index in 0 until length) data[index] else 0.0
		@Suppress("ConvertTwoComparisonsToRangeCheck") // @TODO: Kotlin native doesn't optimize this in release!
		return if (index >= 0 && index < length) data[index] else 0f
	}
	operator fun set(index: Int, value: Float) = run {
		if (index >= length) {
			ensure(index + 1)
			length = index + 1
		}
		data[index] = value
	}

	override fun iterator(): Iterator<Float> = data.take(length).iterator()

	override fun contains(element: Float): Boolean {
		for (n in 0 until length) if (this.data[n] == element) return true
		return false
	}

	override fun containsAll(elements: Collection<Float>): Boolean {
		for (e in elements) if (!contains(e)) return false
		return true
	}

	@Suppress("ReplaceSizeZeroCheckWithIsEmpty")
	override fun isEmpty(): Boolean = this.size == 0

	fun indexOf(value: Float, start: Int = 0, end: Int = this.size): Int {
		for (n in start until end) if (data[n] == value) return n
		return -1
	}

	fun removeAt(index: Int): Float {
		if (index < 0 || index >= length) throw IndexOutOfBoundsException()
		val out = data[index]
		if (index < length - 1) MemTools.arraycopy(data, index + 1, data, index, length - index - 1)
		length--
		return out
	}

	fun getFloat(index: Int): Float = this[index]
	fun setFloat(index: Int, value: Float) = run { this[index] = value }

	override fun getDouble(index: Int): Double = this[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { this[index] = value.toFloat() }

	fun toFloatArray() = this.data.copyOf(length)
}

//fun FloatArrayList.binarySearch(value: Float) = data.binarySearch(value, 0, size)
fun floatArrayListOf(vararg floats: Float) = FloatArrayList(*floats)

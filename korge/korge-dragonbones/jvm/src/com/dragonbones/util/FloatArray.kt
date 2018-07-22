package com.dragonbones.util

import java.util.*

open class FloatArray : NumberArray<Float> {
	private var _length: Int = 0
	private var data: FloatArray? = null

	protected constructor(none: Boolean) {}

	constructor() : this(EMPTY, 0) {}

	constructor(length: Int) : this(FloatArray(length), length) {}

	@JvmOverloads constructor(data: FloatArray, length: Int = data.size) {
		this.data = data
		this.length = length
	}

	private fun ensureCapacity(minLength: Int) {
		if (data!!.size < minLength) {
			data = Arrays.copyOf(data!!, Math.max(16, Math.max(minLength, data!!.size * 3)))
		}
	}

	open operator fun get(index: Int): Float {
		return data!![index]
	}

	open operator fun set(index: Int, value: Float) {
		data[index] = value
	}

	override fun getObject(index: Int): Float? {
		return get(index)
	}

	override fun setObject(index: Int, value: Float?) {
		set(index, value!!)
	}

	override fun create(count: Int): ArrayBase<Float> {
		return FloatArray(count)
	}

	fun push(value: Float) {
		val pos = length
		setLength(pos + 1)
		data[pos] = value
	}

	var length: Int
		get() = _length
		set(value) {
			this._length = length
			ensureCapacity(length)
		}

	override fun copy(): FloatArray {
		val out = FloatArray()
		out.length = length
		out.data = Arrays.copyOf(data!!, data!!.size)
		return out
	}

	fun sort(start: Int, end: Int) {
		Arrays.sort(data, start, end)
	}

	override fun sort(comparator: Comparator<Float>, start: Int, end: Int) {
		throw RuntimeException("Not implemented")
	}

	companion object {
		private val EMPTY = FloatArray(0)
	}
}

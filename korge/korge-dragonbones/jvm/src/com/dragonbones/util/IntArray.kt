package com.dragonbones.util

import java.util.Arrays
import java.util.Comparator

open class IntArray : NumberArray<Int> {
    private var length: Int = 0
    private var data: IntArray? = null

    protected constructor(none: Boolean) {}

    constructor() : this(EMPTY, 0) {}

    constructor(length: Int) : this(IntArray(length), length) {}

    @JvmOverloads constructor(data: IntArray, length: Int = data.size) {
        this.data = data
        this.length = length
    }

    private fun ensureCapacity(minLength: Int) {
        if (data!!.size < minLength) {
            data = Arrays.copyOf(data!!, Math.max(16, Math.max(minLength, data!!.size * 3)))
        }
    }

    open operator fun get(index: Int): Int {
        return data!![index]
    }

    open operator fun set(index: Int, value: Int) {
        data[index] = value
    }

    override fun getObject(index: Int): Int? {
        return get(index)
    }

    override fun setObject(index: Int, value: Int?) {
        set(index, value!!)
    }

    override fun create(count: Int): ArrayBase<Int> {
        return IntArray(count)
    }

    fun push(value: Int) {
        val pos = getLength()
        setLength(pos + 1)
        data[pos] = value
    }

    override fun getLength(): Int {
        return length
    }

    override fun setLength(length: Int) {
        this.length = length
        ensureCapacity(length)
    }

    protected open fun createInstance(): IntArray {
        return IntArray()
    }

    override fun copy(): IntArray {
        val out = createInstance()
        out.length = length
        out.data = Arrays.copyOf(data!!, data!!.size)
        return out
    }

    fun sort(start: Int, end: Int) {
        Arrays.sort(data, start, end)
    }

    override fun sort(comparator: Comparator<Int>, start: Int, end: Int) {
        throw RuntimeException("Not implemented")
    }

    companion object {
        private val EMPTY = IntArray(0)
    }
}

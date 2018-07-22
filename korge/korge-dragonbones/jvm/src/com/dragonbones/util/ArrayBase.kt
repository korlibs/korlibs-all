package com.dragonbones.util

import java.util.Comparator
import java.util.Objects

abstract class ArrayBase<T> : Iterable<T> {
    abstract var length: Int

    abstract fun getObject(index: Int): T

    abstract fun setObject(index: Int, value: T)

    abstract fun create(count: Int): ArrayBase<T>

    fun length(): Int {
        return length
    }

    fun size(): Int {
        return length
    }

    fun clear() {
        length = 0
    }

    fun incrementLength(delta: Int) {
        length = length + delta
    }

    // @TODO: Optimize this!
    fun indexOf(value: T): Int {
        return indexOfObject(value)
    }

    fun indexOfObject(value: T): Int {
        for (n in 0 until length()) {
            if (getObject(n) == value) {
                return n
            }
        }
        return -1
    }

    fun pushObject(value: T) {
        incrementLength(1)
        setObject(length - 1, value)
    }

    fun popObject(): T {
        val out = getObject(length - 1)
        incrementLength(-1)
        return out
    }

    fun unshiftObject(item: T) {
        length = length + 1
        for (n in 1 until length) {
            setObject(n - 1, getObject(n))
        }
        setObject(0, item)
    }

    override fun iterator(): Iterator<T> {
        val pos = intArrayOf(0)
        return object : Iterator<T> {
            override fun hasNext(): Boolean {
                return pos[0] < length
            }

            override fun next(): T {
                return getObject(pos[0]++)
            }
        }
    }

    fun splice(index: Int, removeCount: Int, vararg addItems: T) {
        var addItems = addItems
        val ref = copy()
        if (addItems == null) addItems = EMPTY_ARRAY as Array<T>
        length = length - removeCount + addItems.size
        for (n in 0 until index) {
            this.setObject(n, ref.getObject(n))
        }
        for (n in addItems.indices) {
            this.setObject(index + n, addItems[n])
        }
        for (n in 0 until ref.length() - removeCount) {
            this.setObject(index + addItems.size + n, ref.getObject(index + removeCount + n))
        }
    }

    open fun copy(): ArrayBase<T> {
        return slice()
    }

    @JvmOverloads
    fun slice(start: Int = 0, end: Int = length): ArrayBase<T> {
        val count = end - start
        val out = create(count)
        for (n in 0 until count) {
            out.setObject(n, this.getObject(start + n))
        }
        return out
    }

    fun concat(vararg items: T): ArrayBase<T> {
        val out = create(this.length() + items.size)
        for (n in 0 until this.length()) {
            out.setObject(n, this.getObject(n))
        }
        for (n in items.indices) {
            out.setObject(this.length() + n, items[n])
        }
        return out
    }

    fun sort(comparator: Comparator<T>) {
        sort(comparator, 0, length)
    }

    abstract fun sort(comparator: Comparator<T>, start: Int, end: Int)

    companion object {

        private val EMPTY_ARRAY = arrayOfNulls<Any>(0)
    }
}

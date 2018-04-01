package com.soywiz.kds

class IntStack() {
    private val items = IntArrayList()

    val size: Int get() = items.size
    val hasMore: Boolean get() = size > 0
    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0

    constructor(vararg items: Int) : this() {
        for (item in items) push(item)
    }

    fun push(v: Int) {
        items.add(v)
    }

    fun pop(): Int = items.removeAt(items.size - 1)
}

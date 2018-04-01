package com.soywiz.kds

class IntQueue() {
    private val items = IntCircularList()

    val size: Int get() = items.size
    val hasMore: Boolean get() = size > 0
    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0

    constructor(vararg items: Int) : this() {
        for (item in items) enqueue(item)
    }

    fun enqueue(v: Int) {
        items.addLast(v)
    }

    fun dequeue(): Int = items.removeFirst()
}

package com.soywiz.kds

class Pool<T>(private val reset: (T) -> Unit = {}, preallocate: Int = 0, private val gen: (Int) -> T) {
    constructor(preallocate: Int = 0, gen: (Int) -> T) : this({}, preallocate, gen)

    private val items = CircularList<T>()
    private var lastId = 0

    val itemsInPool: Int get() = items.size

    init {
        for (n in 0 until preallocate) items += gen(lastId++)
    }

    fun alloc(): T {
        if (items.isNotEmpty()) {
            return items.removeLast()
        } else {
            return gen(lastId++)
        }
    }

    fun free(v: T) {
        reset(v)
        items.addFirst(v)
    }

    fun free(v: Iterable<T>) {
        for (it in v) reset(it)
        items.addAll(v)
    }

    inline fun <T2> alloc(crossinline callback: (T) -> T2): T2 {
        val temp = alloc()
        try {
            return callback(temp)
        } finally {
            free(temp)
        }
    }
}
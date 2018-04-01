package com.soywiz.kds

class IntFloatMap {
    private val i = IntIntMap()

    val size: Int get() = i.size
    fun clear() = i.clear()
    fun remove(key: Int) = i.remove(key)

    val keys get() = i.keys
    val values
        get() = object {
            operator fun iterator() = object {
                val it = i.values.iterator()
                operator fun hasNext() = it.hasNext()
                operator fun next() = Float.fromBits(it.next())
            }
        }
    val entries
        get() = object {
            operator fun iterator() = object {
                val it = i.entries.iterator()
                operator fun hasNext() = it.hasNext()
                operator fun next() = it.next().let { Entry(it.key, Float.fromBits(it.value)) }
            }
        }

    data class Entry(val key: Int, val value: Float)

    operator fun contains(key: Int): Boolean = key in i
    operator fun get(key: Int): Float = Float.fromBits(i[key])
    operator fun set(key: Int, value: Float): Float = Float.fromBits(i.set(key, value.toRawBits()))
}
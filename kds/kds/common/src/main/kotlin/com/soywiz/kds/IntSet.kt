package com.soywiz.kds

class IntSet {
    private val data = IntMap<Boolean>()

    fun clear() = run { data.clear() }
    fun add(item: Int) = run { data[item] = true }
    fun contains(item: Int) = data[item] == true
    fun remove(item: Int) = run { data.remove(item) }
}
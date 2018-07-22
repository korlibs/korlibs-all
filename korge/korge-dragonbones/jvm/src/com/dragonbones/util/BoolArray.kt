package com.dragonbones.util

class BoolArray : IntArray() {
    override fun createInstance(): IntArray {
        return BoolArray()
    }

    fun getBool(index: Int): Boolean {
        return get(index) != 0
    }

    fun setBool(index: Int, value: Boolean) {
        set(index, if (value) 1 else 0)
    }
}

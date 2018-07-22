package com.dragonbones.util.buffer

import com.dragonbones.util.ShortArray

class Int16Array(private val buffer: ArrayBuffer, private val offset: Int, private val count: Int) : ShortArray(false),
    ArrayBufferView {
    private val byteOffset: Int

    init {
        this.byteOffset = offset * BYTES_PER_ELEMENT
    }

    override fun get(index: Int): Int {
        return buffer.getS16(byteOffset + index * BYTES_PER_ELEMENT)
    }

    override fun set(i: Int, v: Int) {
        buffer.setS16(byteOffset + i * BYTES_PER_ELEMENT, v)
    }

    companion object {
        val BYTES_PER_ELEMENT = 2
    }
}

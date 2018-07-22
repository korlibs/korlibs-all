package com.dragonbones.util.buffer

import com.dragonbones.util.CharArray

class Uint16Array(private val buffer: ArrayBuffer, private val offset: Int, private val count: Int) : CharArray(false),
    ArrayBufferView {
    private val byteOffset: Int

    override var length: Int
        get() = count
        set(length) {}

    init {
        this.byteOffset = offset * BYTES_PER_ELEMENT
    }

    override fun get(index: Int): Int {
        return buffer.getU16(byteOffset + index * BYTES_PER_ELEMENT)
    }

    override fun set(index: Int, value: Int) {
        buffer.setU16(byteOffset + index * BYTES_PER_ELEMENT, value)
    }

    companion object {
        val BYTES_PER_ELEMENT = 2
    }
}

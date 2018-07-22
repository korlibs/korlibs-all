package com.dragonbones.util.buffer

import com.dragonbones.util.IntArray

class Uint32Array(private val buffer: ArrayBuffer, private val offset: Int, private val count: Int) : IntArray(false),
    ArrayBufferView {
    private val byteOffset: Int

    init {
        this.byteOffset = offset * BYTES_PER_ELEMENT
    }

    override fun get(index: Int): Int {
        return buffer.getU32(byteOffset + index * BYTES_PER_ELEMENT)
    }

    companion object {
        val BYTES_PER_ELEMENT = 4
    }
}

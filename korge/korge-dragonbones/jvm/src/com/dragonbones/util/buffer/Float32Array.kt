package com.dragonbones.util.buffer

import com.dragonbones.util.FloatArray

class Float32Array(private val buffer: ArrayBuffer, private val offset: Int, private val count: Int) :
    FloatArray(false) {
    private val byteOffset: Int

    init {
        this.byteOffset = offset * BYTES_PER_ELEMENT
    }

    override fun get(index: Int): Float {
        return buffer.getF32(byteOffset + index * BYTES_PER_ELEMENT)
    }

    override fun set(index: Int, value: Float) {
        buffer.setF32(byteOffset + index * BYTES_PER_ELEMENT, value)
    }

    companion object {
        val BYTES_PER_ELEMENT = 4
    }
}

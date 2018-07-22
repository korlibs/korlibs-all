package com.dragonbones.util.buffer

class Uint8Array(private val buffer: ArrayBuffer, private val offset: Int, private val count: Int) : ArrayBufferView {
    private val byteOffset: Int

    init {
        this.byteOffset = offset
    }

    fun length(): Int {
        return count
    }

    operator fun get(index: Int): Int {
        return buffer.getU8(byteOffset + index)
    }

    companion object {
        val BYTES_PER_ELEMENT = 1
    }
}

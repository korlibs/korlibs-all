package com.dragonbones.util.buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder

class ArrayBuffer {
    var data: ByteBuffer

    constructor(data: ByteArray) {
        this.data = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder())
    }

    constructor(length: Int) {
        this.data = ByteBuffer.allocate(length).order(ByteOrder.nativeOrder())
    }

    fun getU8(i: Int): Int {
        return data.get(i).toInt()
    }

    fun getU32(i: Int): Int {
        return data.getInt(i)
    }

    fun getF32(i: Int): Float {
        return data.getFloat(i)
    }

    fun getU16(i: Int): Int {
        return data.getChar(i).toInt()
    }

    fun getS16(i: Int): Int {
        return data.getShort(i).toInt()
    }

    fun setS16(i: Int, v: Int) {
        data.putShort(i, v.toShort())
    }

    fun setF32(i: Int, value: Float) {
        data.putFloat(i, value)
    }

    fun setU16(i: Int, value: Int) {
        data.putChar(i, value.toChar())
    }

    fun getBytes(i: Int, count: Int): ByteArray {
        val bytes = ByteArray(count)
        for (n in 0 until count) {
            bytes[n] = getU8(i + n).toByte()
        }
        return bytes
    }
}

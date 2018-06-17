package com.soywiz.kmedialayer

import org.khronos.webgl.*

actual class KmlNativeBuffer constructor(val arrayBuffer: ArrayBuffer) {
   val arrayByte = Int8Array(arrayBuffer)
   val arrayUByte = Uint8Array(arrayBuffer)
   val arrayShort = Int16Array(arrayBuffer)
   val arrayInt = Int32Array(arrayBuffer)
   val arrayFloat = Float32Array(arrayBuffer)
   actual val size: Int = arrayBuffer.byteLength
   actual constructor(size: Int) : this(ArrayBuffer(kmlInternalRoundUp(size, 8)))

    actual fun getByte(index: Int): Byte = arrayByte[index]
    actual fun setByte(index: Int, value: Byte): Unit = run { arrayByte[index] = value }
    actual fun getShort(index: Int): Short = arrayShort[index]
    actual fun setShort(index: Int, value: Short): Unit = run { arrayShort[index] = value }
    actual fun getInt(index: Int): Int = arrayInt[index]
    actual fun setInt(index: Int, value: Int): Unit = run { arrayInt[index] = value }
    actual fun getFloat(index: Int): Float = arrayFloat[index]
    actual fun setFloat(index: Int, value: Float): Unit = run { arrayFloat[index] = value }
    actual fun dispose() = Unit
}
@Suppress("USELESS_CAST") val KmlNativeBuffer.arrayBuffer: ArrayBuffer get() = (this as KmlNativeBuffer).arrayBuffer
@Suppress("USELESS_CAST") val KmlNativeBuffer.arrayByte: Int8Array get() = (this as KmlNativeBuffer).arrayByte
@Suppress("USELESS_CAST") val KmlNativeBuffer.arrayUByte: Uint8Array get() = (this as KmlNativeBuffer).arrayUByte
@Suppress("USELESS_CAST") val KmlNativeBuffer.arrayShort: Int16Array get() = (this as KmlNativeBuffer).arrayShort
@Suppress("USELESS_CAST") val KmlNativeBuffer.arrayInt: Int32Array get() = (this as KmlNativeBuffer).arrayInt
@Suppress("USELESS_CAST") val KmlNativeBuffer.arrayFloat: Float32Array get() = (this as KmlNativeBuffer).arrayFloat

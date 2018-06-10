package com.soywiz.kmem

/*
class KmlNativeBuffer(val mem: MemBuffer) {
	val size: Int get() = mem.size

	val arrayByte = mem.asInt8Buffer()
	val arrayUByte = UInt8Buffer(arrayByte)
	val arrayShort = mem.asInt16Buffer()
	val arrayInt = mem.asInt32Buffer()
	val arrayFloat = mem.asFloat32Buffer()
	val arrayDouble = mem.asFloat64Buffer()

	constructor(size: Int) : this(MemBufferAlloc(size))

	fun getByte(index: Int): Byte = arrayByte[index]
	fun setByte(index: Int, value: Byte): Unit = run { arrayByte[index] = value }
	fun getShort(index: Int): Short = arrayShort[index]
	fun setShort(index: Int, value: Short): Unit = run { arrayShort[index] = value }
	fun getInt(index: Int): Int = arrayInt[index]
	fun setInt(index: Int, value: Int): Unit = run { arrayInt[index] = value }
	fun getFloat(index: Int): Float = arrayFloat[index]
	fun setFloat(index: Int, value: Float): Unit = run { arrayFloat[index] = value }
	fun dispose() = Unit
}

class UInt8Buffer(val b: Int8Buffer) {
	operator fun get(index: Int): Int = b[index].toInt() and 0xFF
	operator fun set(index: Int, value: Int): Unit = run { b[index] = value.toByte() }
}
*/

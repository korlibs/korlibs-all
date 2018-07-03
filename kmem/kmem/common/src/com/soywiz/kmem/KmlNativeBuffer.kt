package com.soywiz.kmem

class KmlNativeBuffer(val mem: MemBuffer) {
	constructor(size: Int) : this(MemBufferAlloc(size))

	val size: Int get() = mem.size

	val data = mem.getData()
	val arrayByte = mem.asInt8Buffer()
	//val arrayUByte = UInt8Buffer(arrayByte)
	val arrayShort = mem.asInt16Buffer()
	val arrayInt = mem.asInt32Buffer()
	val arrayFloat = mem.asFloat32Buffer()
	val arrayDouble = mem.asFloat64Buffer()

	fun getByte(index: Int): Byte = arrayByte[index]
	fun getShort(index: Int): Short = arrayShort[index]
	fun getInt(index: Int): Int = arrayInt[index]
	fun getFloat(index: Int): Float = arrayFloat[index]
	fun getDouble(index: Int): Double = arrayDouble[index]

	fun setByte(index: Int, value: Byte): Unit = run { arrayByte[index] = value }
	fun setShort(index: Int, value: Short): Unit = run { arrayShort[index] = value }
	fun setInt(index: Int, value: Int): Unit = run { arrayInt[index] = value }
	fun setFloat(index: Int, value: Float): Unit = run { arrayFloat[index] = value }
	fun setDouble(index: Int, value: Double): Unit = run { arrayDouble[index] = value }

	fun dispose() = Unit
}

class UInt8Buffer(val b: Int8Buffer) {
	operator fun get(index: Int): Int = b[index].toInt() and 0xFF
	operator fun set(index: Int, value: Int): Unit = run { b[index] = value.toByte() }
}

inline fun <T> kmlNativeBuffer(size: Int, callback: (KmlNativeBuffer) -> T): T = KmlNativeBuffer(size).run(callback)


fun KmlNativeBuffer.setFloats(offset: Int, data: FloatArray, dataOffset: Int, count: Int) = this.apply {
	for (n in 0 until count) this.setFloat(offset + n, data[dataOffset + n])
}

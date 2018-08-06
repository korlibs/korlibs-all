package com.soywiz.kmem

class KmlNativeBuffer(val mem: MemBuffer, val size: Int = mem.size) {
	constructor(size: Int) : this(MemBufferAlloc(size.sizeAligned()), size)
	constructor(size: Int, direct: Boolean) : this(if (direct) MemBufferAlloc(size.sizeAligned()) else MemBufferAllocNoDirect(size.sizeAligned()), size)

	//(size + 0xF) and 0xF.inv()

	val buffer get() = mem
	val data = mem.getData()
	val arrayByte = mem.asInt8Buffer()
	//val arrayUByte = UInt8Buffer(arrayByte)
	val arrayShort = mem.asInt16Buffer()
	val arrayInt = mem.asInt32Buffer()
	val arrayFloat = mem.asFloat32Buffer()
	//val arrayDouble = mem.asFloat64Buffer()

	val i8 get() = arrayByte
	val i16 get() = arrayShort
	val i32 get() = arrayInt
	val f32 get() = arrayFloat

	fun getByte(index: Int): Byte = arrayByte[index]
	fun getShort(index: Int): Short = arrayShort[index]
	fun getInt(index: Int): Int = arrayInt[index]
	fun getFloat(index: Int): Float = arrayFloat[index]
	//fun getDouble(index: Int): Double = arrayDouble[index]

	fun setByte(index: Int, value: Byte): Unit = run { arrayByte[index] = value }
	fun setShort(index: Int, value: Short): Unit = run { arrayShort[index] = value }
	fun setInt(index: Int, value: Int): Unit = run { arrayInt[index] = value }
	fun setFloat(index: Int, value: Float): Unit = run { arrayFloat[index] = value }
	//fun setDouble(index: Int, value: Double): Unit = run { arrayDouble[index] = value }

	fun dispose() = Unit


	companion object {
		//const val ALIGNMENT = 4
		//private fun Int.sizeAligned() = (this + 0xF) and 0xF.inv()
		private fun Int.sizeAligned() = (this + 0x3) and 0x3.inv()

		fun alloc(size: Int): KmlNativeBuffer = KmlNativeBuffer(MemBufferAlloc(size.sizeAligned()), size)
		fun wrap(buffer: MemBuffer, size: Int = buffer.size): KmlNativeBuffer = KmlNativeBuffer(buffer, size)
		fun wrap(array: ByteArray): KmlNativeBuffer = KmlNativeBuffer(MemBufferWrap(array), array.size)

		operator fun invoke(size: Int): KmlNativeBuffer = KmlNativeBuffer(MemBufferAlloc(size.sizeAligned()), size)
		operator fun invoke(buffer: MemBuffer, size: Int = buffer.size): KmlNativeBuffer = KmlNativeBuffer(buffer, size)
		operator fun invoke(array: ByteArray): KmlNativeBuffer = KmlNativeBuffer(MemBufferWrap(array), array.size)

		fun copy(src: KmlNativeBuffer, srcPos: Int, dst: KmlNativeBuffer, dstPos: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPos, dst.buffer, dstPos, length)

		fun copy(src: KmlNativeBuffer, srcPos: Int, dst: ByteArray, dstPos: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPos, dst, dstPos, length)

		fun copy(src: ByteArray, srcPos: Int, dst: KmlNativeBuffer, dstPos: Int, length: Int): Unit =
			arraycopy(src, srcPos, dst.buffer, dstPos, length)

		fun copyAligned(src: KmlNativeBuffer, srcPosAligned: Int, dst: ShortArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

		fun copyAligned(src: ShortArray, srcPosAligned: Int, dst: KmlNativeBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)

		fun copyAligned(src: KmlNativeBuffer, srcPosAligned: Int, dst: IntArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

		fun copyAligned(src: IntArray, srcPosAligned: Int, dst: KmlNativeBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)

		fun copyAligned(src: KmlNativeBuffer, srcPosAligned: Int, dst: FloatArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

		fun copyAligned(src: FloatArray, srcPosAligned: Int, dst: KmlNativeBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)
	}

	operator fun get(index: Int): Int = i8[index].toInt() and 0xFF
	operator fun set(index: Int, value: Int): Unit = run { i8[index] = value.toByte() }

	fun setAlignedInt16(index: Int, value: Short): Unit = run { i16[index] = value }
	fun getAlignedInt16(index: Int): Short = i16[index]
	fun setAlignedInt32(index: Int, value: Int): Unit = run { i32[index] = value }
	fun getAlignedInt32(index: Int): Int = i32[index]
	fun setAlignedFloat32(index: Int, value: Float): Unit = run { f32[index] = value }
	fun getAlignedFloat32(index: Int): Float = f32[index]

	fun getUnalignedInt16(index: Int): Short = data.getShort(index)
	fun setUnalignedInt16(index: Int, value: Short): Unit = run { data.setShort(index, value) }
	fun setUnalignedInt32(index: Int, value: Int): Unit = run { data.setInt(index, value) }
	fun getUnalignedInt32(index: Int): Int = data.getInt(index)
	fun setUnalignedFloat32(index: Int, value: Float): Unit = run { data.setFloat(index, value) }
	fun getUnalignedFloat32(index: Int): Float = data.getFloat(index)

	fun setArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int) = copy(src, srcPos, this, dstPos, len)
	fun setAlignedArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int) = copy(src, srcPos, this, dstPos, len)
	fun setAlignedArrayInt16(dstPos: Int, src: ShortArray, srcPos: Int, len: Int) =
		copyAligned(src, srcPos, this, dstPos, len)

	fun setAlignedArrayInt32(dstPos: Int, src: IntArray, srcPos: Int, len: Int) =
		copyAligned(src, srcPos, this, dstPos, len)

	fun setAlignedArrayFloat32(dstPos: Int, src: FloatArray, srcPos: Int, len: Int) =
		copyAligned(src, srcPos, this, dstPos, len)

	fun getArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) = copy(this, srcPos, dst, dstPos, len)
	fun getAlignedArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) = copy(this, srcPos, dst, dstPos, len)
	fun getAlignedArrayInt16(srcPos: Int, dst: ShortArray, dstPos: Int, len: Int) =
		copyAligned(this, srcPos, dst, dstPos, len)

	fun getAlignedArrayInt32(srcPos: Int, dst: IntArray, dstPos: Int, len: Int) =
		copyAligned(this, srcPos, dst, dstPos, len)

	fun getAlignedArrayFloat32(srcPos: Int, dst: FloatArray, dstPos: Int, len: Int) =
		copyAligned(this, srcPos, dst, dstPos, len)
}

class Uint8Buffer(val b: Int8Buffer) {
	companion object;

	val size: Int get() = b.size
	operator fun get(index: Int): Int = b[index].toInt() and 0xFF
	operator fun set(index: Int, value: Int): Unit = run { b[index] = value.toByte() }
}

class Uint16Buffer(val b: Int16Buffer) {
	companion object;

	val size: Int get() = b.size
	operator fun get(index: Int): Int = b[index].toInt() and 0xFFFF
	operator fun set(index: Int, value: Int): Unit = run { b[index] = value.toShort() }
}

fun Uint8BufferAlloc(size: Int): Uint8Buffer = Uint8Buffer(Int8BufferAlloc(size))
fun Uint16BufferAlloc(size: Int): Uint16Buffer = Uint16Buffer(Int16BufferAlloc(size))

inline fun <T> kmlNativeBuffer(size: Int, callback: (KmlNativeBuffer) -> T): T = KmlNativeBuffer(size).run(callback)


fun KmlNativeBuffer.setFloats(offset: Int, data: FloatArray, dataOffset: Int, count: Int) = this.apply {
	for (n in 0 until count) this.setFloat(offset + n, data[dataOffset + n])
}

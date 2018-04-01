package com.soywiz.kmem

class FastMemory(val buffer: MemBuffer, val size: Int) {
	val data = buffer.getData()
	val i8 = buffer.asInt8Buffer()
	val i16 = buffer.asInt16Buffer()
	val i32 = buffer.asInt32Buffer()
	val f32 = buffer.asFloat32Buffer()

	companion object {
		fun alloc(size: Int): FastMemory = FastMemory(MemBufferAlloc((size + 0xF) and 0xF.inv()), size)
		fun wrap(buffer: MemBuffer, size: Int = buffer.size): FastMemory = FastMemory(buffer, size)
		fun wrap(array: ByteArray): FastMemory = FastMemory(MemBufferWrap(array), array.size)

		operator fun invoke(size: Int): FastMemory = FastMemory(MemBufferAlloc((size + 0xF) and 0xF.inv()), size)
		operator fun invoke(buffer: MemBuffer, size: Int = buffer.size): FastMemory = FastMemory(buffer, size)
		operator fun invoke(array: ByteArray): FastMemory = FastMemory(MemBufferWrap(array), array.size)

		fun copy(src: FastMemory, srcPos: Int, dst: FastMemory, dstPos: Int, length: Int): Unit = arraycopy(src.buffer, srcPos, dst.buffer, dstPos, length)
		fun copy(src: FastMemory, srcPos: Int, dst: ByteArray, dstPos: Int, length: Int): Unit = arraycopy(src.buffer, srcPos, dst, dstPos, length)
		fun copy(src: ByteArray, srcPos: Int, dst: FastMemory, dstPos: Int, length: Int): Unit = arraycopy(src, srcPos, dst.buffer, dstPos, length)
		fun copyAligned(src: FastMemory, srcPosAligned: Int, dst: ShortArray, dstPosAligned: Int, length: Int): Unit = arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)
		fun copyAligned(src: ShortArray, srcPosAligned: Int, dst: FastMemory, dstPosAligned: Int, length: Int): Unit = arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)
		fun copyAligned(src: FastMemory, srcPosAligned: Int, dst: IntArray, dstPosAligned: Int, length: Int): Unit = arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)
		fun copyAligned(src: IntArray, srcPosAligned: Int, dst: FastMemory, dstPosAligned: Int, length: Int): Unit = arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)
		fun copyAligned(src: FastMemory, srcPosAligned: Int, dst: FloatArray, dstPosAligned: Int, length: Int): Unit = arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)
		fun copyAligned(src: FloatArray, srcPosAligned: Int, dst: FastMemory, dstPosAligned: Int, length: Int): Unit = arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)
	}

	operator fun get(index: Int): Int = i8[index].toInt() and 0xFF
	operator fun set(index: Int, value: Int): Unit = run { i8[index] = value.toByte() }

	fun setAlignedInt16(index: Int, value: Short): Unit = run { i16[index] = value }
	fun getAlignedInt16(index: Int): Short = i16[index]
	fun setAlignedInt32(index: Int, value: Int): Unit = run { i32[index] = value }
	fun getAlignedInt32(index: Int): Int = i32[index]
	fun setAlignedFloat32(index: Int, value: Float): Unit = run { f32[index] = value }
	fun getAlignedFloat32(index: Int): Float = f32[index]

	fun getInt16(index: Int): Short = data.getShort(index)
	fun setInt16(index: Int, value: Short): Unit = run { data.setShort(index, value) }
	fun setInt32(index: Int, value: Int): Unit = run { data.setInt(index, value) }
	fun getInt32(index: Int): Int = data.getInt(index)
	fun setFloat32(index: Int, value: Float): Unit = run { data.setFloat(index, value) }
	fun getFloat32(index: Int): Float = data.getFloat(index)

	fun setArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int) = copy(src, srcPos, this, dstPos, len)
	fun setAlignedArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int) = copy(src, srcPos, this, dstPos, len)
	fun setAlignedArrayInt16(dstPos: Int, src: ShortArray, srcPos: Int, len: Int) = copyAligned(src, srcPos, this, dstPos, len)
	fun setAlignedArrayInt32(dstPos: Int, src: IntArray, srcPos: Int, len: Int) = copyAligned(src, srcPos, this, dstPos, len)
	fun setAlignedArrayFloat32(dstPos: Int, src: FloatArray, srcPos: Int, len: Int) = copyAligned(src, srcPos, this, dstPos, len)

	fun getArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) = copy(this, srcPos, dst, dstPos, len)
	fun getAlignedArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) = copy(this, srcPos, dst, dstPos, len)
	fun getAlignedArrayInt16(srcPos: Int, dst: ShortArray, dstPos: Int, len: Int) = copyAligned(this, srcPos, dst, dstPos, len)
	fun getAlignedArrayInt32(srcPos: Int, dst: IntArray, dstPos: Int, len: Int) = copyAligned(this, srcPos, dst, dstPos, len)
	fun getAlignedArrayFloat32(srcPos: Int, dst: FloatArray, dstPos: Int, len: Int) = copyAligned(this, srcPos, dst, dstPos, len)
}

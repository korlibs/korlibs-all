package com.soywiz.kmem

import java.nio.*

actual class UnsafeFastMemory actual constructor(actual val size: Int) {
	val buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
	val i8 = buffer
	val i16 = buffer.asShortBuffer()
	val i32 = buffer.asIntBuffer()
	val f32 = buffer.asFloatBuffer()
	val f64 = buffer.asDoubleBuffer()

	actual inline fun alignedGetS8(offset: Int): Byte = i8[offset]
	actual inline fun alignedGetS16(offset: Int): Short = i16[offset]
	actual inline fun alignedGetS32(offset: Int): Int = i32[offset]
	actual inline fun alignedGetF32(offset: Int): Float = f32[offset]
	actual inline fun alignedGetF64(offset: Int): Double = f64[offset]

	actual inline fun alignedSetS8(offset: Int, value: Byte) = Unit.apply { i8.put(offset, value) }
	actual inline fun alignedSetS16(offset: Int, value: Short) = Unit.apply { i16.put(offset, value) }
	actual inline fun alignedSetS32(offset: Int, value: Int) = Unit.apply { i32.put(offset, value) }
	actual inline fun alignedSetF32(offset: Int, value: Float) = Unit.apply { f32.put(offset, value) }
	actual inline fun alignedSetF64(offset: Int, value: Double) = Unit.apply { f64.put(offset, value) }

	actual fun close() {
		buffer.clear()
	}
}
package com.soywiz.kmem

import org.khronos.webgl.*

actual class UnsafeFastMemory actual constructor(actual val size: Int) {
	val buffer = ArrayBuffer(size)
	val i8 = Int8Array(buffer)
	val i16 = Int16Array(buffer)
	val i32 = Int32Array(buffer)
	val f32 = Float32Array(buffer)
	val f64 = Float64Array(buffer)

	actual inline fun alignedGetS8(offset: Int): Byte = i8[offset]
	actual inline fun alignedGetS16(offset: Int): Short = i16[offset]
	actual inline fun alignedGetS32(offset: Int): Int = i32[offset]
	actual inline fun alignedGetF32(offset: Int): Float = f32[offset]
	actual inline fun alignedGetF64(offset: Int): Double = f64[offset]

	actual inline fun alignedSetS8(offset: Int, value: Byte) = run { i8[offset] = value }
	actual inline fun alignedSetS16(offset: Int, value: Short) = run { i16[offset] = value }
	actual inline fun alignedSetS32(offset: Int, value: Int) = run { i32[offset] = value }
	actual inline fun alignedSetF32(offset: Int, value: Float) = run { f32[offset] = value }
	actual inline fun alignedSetF64(offset: Int, value: Double) = run { f64[offset] = value }

	actual fun close() {
	}
}
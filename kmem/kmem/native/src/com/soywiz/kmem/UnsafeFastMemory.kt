package com.soywiz.kmem

import kotlinx.cinterop.*

actual class UnsafeFastMemory actual constructor(actual val size: Int) {
	@PublishedApi
	internal val data = ByteArray(size)
	@PublishedApi
	internal val pin = data.pin()
	@PublishedApi
	internal val i8 = pin.addressOf(0).reinterpret<ByteVar>()
	@PublishedApi
	internal val i16 = pin.addressOf(0).reinterpret<ShortVar>()
	@PublishedApi
	internal val i32 = pin.addressOf(0).reinterpret<IntVar>()
	@PublishedApi
	internal val f32 = pin.addressOf(0).reinterpret<FloatVar>()
	@PublishedApi
	internal val f64 = pin.addressOf(0).reinterpret<DoubleVar>()

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
		pin.unpin()
	}
}

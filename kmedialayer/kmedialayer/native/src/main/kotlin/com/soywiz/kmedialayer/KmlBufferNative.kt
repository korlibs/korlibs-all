package com.soywiz.kmedialayer

import konan.*
import kotlinx.cinterop.*

actual class KmlNativeBuffer constructor(
	val placement: NativeFreeablePlacement,
	val ptr: NativePtr,
	actual val size: Int
) {
	actual constructor(size: Int) : this(
		nativeHeap,
		nativeHeap.allocArray<ByteVar>(kmlInternalRoundUp(size, 8)).uncheckedCast(),
		size
	)

	actual inline fun getByte(index: Int): Byte = (ptr + index.toLong() * 1).uncheckedCast<ByteVarOf<Byte>>().value
	actual inline fun setByte(index: Int, value: Byte): Unit {
		(ptr + index.toLong() * 1).uncheckedCast<ByteVarOf<Byte>>().value = value
	}

	actual inline fun getShort(index: Int): Short = (ptr + index.toLong() * 2).uncheckedCast<ShortVarOf<Short>>().value
	actual inline fun setShort(index: Int, value: Short): Unit {
		(ptr + index.toLong() * 2).uncheckedCast<ShortVarOf<Short>>().value = value
	}

	actual inline fun getInt(index: Int): Int = (ptr + index.toLong() * 4).uncheckedCast<IntVarOf<Int>>().value
	actual inline fun setInt(index: Int, value: Int): Unit {
		(ptr + index.toLong() * 4).uncheckedCast<IntVarOf<Int>>().value = value
	}

	actual inline fun getFloat(index: Int): Float = (ptr + index.toLong() * 4).uncheckedCast<FloatVarOf<Float>>().value
	actual inline fun setFloat(index: Int, value: Float): Unit {
		(ptr + index.toLong() * 4).uncheckedCast<FloatVarOf<Float>>().value = value
	}

	actual fun dispose() = run { placement.free(ptr) }
}

fun KmlNativeBuffer.unsafeAddress(): CPointer<ByteVar> = this.ptr.uncheckedCast()

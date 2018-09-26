package com.soywiz.kmem

expect class UnsafeFastMemory constructor(size: Int) {
	val size: Int

	fun alignedGetS8(offset: Int): Byte
	fun alignedGetS16(offset: Int): Short
	fun alignedGetS32(offset: Int): Int
	fun alignedGetF32(offset: Int): Float
	fun alignedGetF64(offset: Int): Double

	fun alignedSetS8(offset: Int, value: Byte)
	fun alignedSetS16(offset: Int, value: Short)
	fun alignedSetS32(offset: Int, value: Int)
	fun alignedSetF32(offset: Int, value: Float)
	fun alignedSetF64(offset: Int, value: Double)

	fun close()
}

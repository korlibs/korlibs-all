@file:Suppress("unused", "RedundantUnitReturnType")

package com.soywiz.kmedialayer

fun kmlInternalRoundUp(n: Int, m: Int) = if (n >= 0) ((n + m - 1) / m) * m else (n / m) * m

expect class KmlNativeBuffer {
	val size: Int

	constructor(size: Int)

	fun getByte(index: Int): Byte
	fun setByte(index: Int, value: Byte): Unit
	fun getShort(index: Int): Short
	fun setShort(index: Int, value: Short): Unit
	fun getInt(index: Int): Int
	fun setInt(index: Int, value: Int): Unit
	fun getFloat(index: Int): Float
	fun setFloat(index: Int, value: Float): Unit
	fun dispose()
}

package com.soywiz.korim.bitmap

import com.soywiz.kmem.*
import com.soywiz.korim.color.*

class Bitmap16(
	width: Int,
	height: Int,
	val data: ShortArray = ShortArray(width * height),
	val format: ColorFormat = RGBA_4444,
	premult: Boolean = false
) : Bitmap(width, height, 16, premult, data) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap =
		Bitmap16(width, height, format = format, premult = premult)

	override operator fun set(x: Int, y: Int, color: Int) = Unit.apply { data[index(x, y)] = color.toShort() }
	override operator fun get(x: Int, y: Int): Int = data[index(x, y)].toInt() and 0xFFFF

	override fun get32(x: Int, y: Int): Int = format.unpackToRGBA(data[index(x, y)].toInt())
	override fun set32(x: Int, y: Int, v: Int) = Unit.apply { data[index(x, y)] = format.packRGBA(v).toShort() }

	override fun copy(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
		val src = this

		val srcArray = src.data
		var srcIndex = src.index(srcX, srcY)
		val srcAdd = src.width

		val dstArray = (dst as Bitmap16).data
		var dstIndex = dst.index(dstX, dstY)
		val dstAdd = dst.width

		for (y in 0 until height) {
			arraycopy(srcArray, srcIndex, dstArray, dstIndex, width)
			srcIndex += srcAdd
			dstIndex += dstAdd
		}
	}

	override fun toString(): String = "Bitmap16($width, $height, format=$format)"
}

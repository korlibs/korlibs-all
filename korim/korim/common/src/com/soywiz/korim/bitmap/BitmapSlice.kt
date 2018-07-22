package com.soywiz.korim.bitmap

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*

interface BmpSlice : Extra {
	val bmp: Bitmap
	val x0: Float
	val y0: Float
	val x1: Float
	val y1: Float
	val x2: Float
	val y2: Float
	val x3: Float
	val y3: Float
	val left: Int
	val top: Int
	val width: Int
	val height: Int
}

class BitmapSlice<out T : Bitmap>(override val bmp: T, val bounds: RectangleInt) : BmpSlice, Extra by Extra.Mixin() {
	override val left get() = bounds.left
	override val top get() = bounds.top
	val right get() = bounds.right
	val bottom get() = bounds.bottom
	override val width get() = bounds.width
	override val height get() = bounds.height

	override val x0 = left.toFloat() / bmp.width.toFloat()
	override val y0 = top.toFloat() / bmp.height.toFloat()

	override val x3 = right.toFloat() / bmp.width.toFloat()
	override val y3 = bottom.toFloat() / bmp.height.toFloat()

	override val x1 = x3
	override val y1 = y0

	override val x2 = x0
	override val y2 = y0

	fun extract(): T = bmp.extract(bounds.x, bounds.y, bounds.width, bounds.height)

	fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): BitmapSlice<T> =
		BitmapSlice(
			bmp, RectangleInt.fromBounds(
				(left + bounds.left).clampX(),
				(top + bounds.top).clampY(),
				(right + bounds.left).clampX(),
				(bottom + bounds.top).clampY()
			)
		)

	fun sliceWithSize(x: Int, y: Int, width: Int, height: Int): BitmapSlice<T> =
		sliceWithBounds(x, y, x + width, y + height)

	fun slice(rect: RectangleInt): BitmapSlice<T> = sliceWithBounds(rect.left, rect.top, rect.right, rect.bottom)
	fun slice(rect: Rectangle): BitmapSlice<T> = slice(rect.toInt())

	private fun Int.clampX() = this.clamp(bounds.left, bounds.right)
	private fun Int.clampY() = this.clamp(bounds.top, bounds.bottom)
}

fun <T : Bitmap> T.slice(): BitmapSlice<T> = BitmapSlice(this, RectangleInt(0, 0, width, height))
fun <T : Bitmap> T.slice(bounds: RectangleInt): BitmapSlice<T> = BitmapSlice<T>(this, bounds)
fun <T : Bitmap> T.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): BitmapSlice<T> =
	BitmapSlice<T>(this, RectangleInt(left, top, right - left, bottom - top))

fun <T : Bitmap> T.sliceWithSize(x: Int, y: Int, width: Int, height: Int): BitmapSlice<T> =
	BitmapSlice<T>(this, RectangleInt(x, y, width, height))


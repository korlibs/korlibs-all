package com.soywiz.korim.bitmap

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*

interface BmpSlice : Extra {
	val bmp: Bitmap
	val tl_x: Float
	val tl_y: Float
	val tr_x: Float
	val tr_y: Float
	val bl_x: Float
	val bl_y: Float
	val br_x: Float
	val br_y: Float
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

	override val tl_x = left.toFloat() / bmp.width.toFloat()
	override val tl_y = top.toFloat() / bmp.height.toFloat()

	override val br_x = right.toFloat() / bmp.width.toFloat()
	override val br_y = bottom.toFloat() / bmp.height.toFloat()

	override val tr_x = br_x
	override val tr_y = tl_y

	override val bl_x = tl_x
	override val bl_y = br_y

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


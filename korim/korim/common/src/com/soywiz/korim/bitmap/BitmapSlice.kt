package com.soywiz.korim.bitmap

import com.soywiz.kmem.*
import com.soywiz.korma.geom.*

class BitmapSlice<out T : Bitmap>(val bmp: T, val bounds: RectangleInt) {
	val left get() = bounds.left
	val top get() = bounds.top
	val right get() = bounds.right
	val bottom get() = bounds.bottom
	val width get() = bounds.width
	val height get() = bounds.height

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

	private fun Int.clampX() = this.clamp(bounds.left, bounds.right)
	private fun Int.clampY() = this.clamp(bounds.top, bounds.bottom)
}
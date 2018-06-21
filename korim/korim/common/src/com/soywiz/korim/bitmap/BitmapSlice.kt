package com.soywiz.korim.bitmap

import com.soywiz.korma.geom.*

class BitmapSlice<out T : Bitmap>(val bmp: T, val bounds: RectangleInt) {
	fun extract(): T = bmp.extract(bounds.x, bounds.y, bounds.width, bounds.height)
}
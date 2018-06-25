package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

// References:
// - https://github.com/memononen/nanosvg/blob/master/src/nanosvgrast.h
// - https://www.geeksforgeeks.org/scan-line-polygon-filling-using-opengl-c/
// - https://hackernoon.com/computer-graphics-scan-line-polygon-fill-algorithm-3cb47283df6
class Bitmap32Context2d(val bmp: Bitmap32) : Context2d.Renderer() {
	override val width: Int get() = bmp.width
	override val height: Int get() = bmp.height

	override fun render(state: Context2d.State, fill: Boolean) {
		println("WARNING: Not implemented context2d on Bitmap32, please use NativeImage instead. Filled the image with PINK.")
		bmp.fill(Colors.PINK)
	}
}
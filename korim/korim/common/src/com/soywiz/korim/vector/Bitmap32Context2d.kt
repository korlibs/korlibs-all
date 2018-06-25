package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.*

class Bitmap32Context2d(val bmp: Bitmap32) : Context2d.Renderer() {
	override val width: Int get() = bmp.width
	override val height: Int get() = bmp.height

	override fun render(state: Context2d.State, fill: Boolean) {
		// @TODO: https://github.com/memononen/nanosvg/blob/master/src/nanosvgrast.h
		TODO("Not implemented context2d on Bitmap32, please use NativeImage instead")
	}
}
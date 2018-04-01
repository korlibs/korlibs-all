package com.soywiz.korui.ui

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.vector.Context2d
import com.soywiz.korui.Application
import com.soywiz.korui.geom.len.pt

suspend fun Container.vectorImage(vector: Context2d.SizedDrawable) = add(VectorImage(this.app).apply {
	setVector(vector, vector.width, vector.height)
})


suspend inline fun Container.vectorImage(vector: Context2d.SizedDrawable, crossinline callback: VectorImage.() -> Unit) = add(VectorImage(this.app).apply {
	setVector(vector, vector.width, vector.height)
	callback(this)
})

class VectorImage(app: Application) : Container(app, LayeredLayout(app)) {
	lateinit var d: Context2d.Drawable
	lateinit var img: Image
	var targetWidth: Int = 512
	var targetHeight: Int = 512

	suspend fun setVector(d: Context2d.Drawable, width: Int, height: Int) {
		this.d = d
		this.targetWidth = width
		this.targetHeight = height
		this.style.defaultSize.setTo(width.pt, height.pt)
		//img = image(raster(width, height))
		img = image(NativeImage(1, 1))
	}

	override fun onResized(x: Int, y: Int, width: Int, height: Int) {
		//println("onResized: $x, $y, $width, $height")
		img.image = raster(width, height)
	}

	fun raster(width: Int, height: Int): NativeImage {
		return NativeImage(
			width, height, d,
			width.toDouble() / this.targetWidth.toDouble(),
			height.toDouble() / this.targetHeight.toDouble()
		)
	}
}

package com.soywiz.korui.ui

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.*
import com.soywiz.korui.*

inline fun Container.vectorImage(vector: Context2d.SizedDrawable, callback: VectorImage.() -> Unit = {}) =
	add(VectorImage(this.app).apply {
		setVector(vector, vector.width, vector.height)
		callback(this)
	})

inline fun Container.vectorImage(
	vector: Context2d.Drawable,
	crossinline callback: VectorImage.() -> Unit = {}
) = add(VectorImage(this.app).apply {
	setVector(vector, null, null)
	callback(this)
})

abstract class BaseCanvas(app: Application) : Container(app, LayeredLayout(app)) {
	private val img = image(NativeImage(1, 1))
	var antialiased = true
	var highDpi = true

	override fun onResized(x: Int, y: Int, width: Int, height: Int) {
		super.onResized(x, y, width, height)
		repaint(width, height)
	}

	override fun repaint() {
		repaint(actualWidth, actualHeight)
	}

	private fun repaint(width: Int, height: Int) {
		val scale = if (highDpi) app.devicePixelRatio else 1.0
		//val scale = 1.0
		val rwidth = (width * scale).toInt()
		val rheight = (height * scale).toInt()
		val image = NativeImage(rwidth, rheight)
		val ctx = image.getContext2d(antialiasing = antialiased).withScaledRenderer(scale)
		//val ctx = image.getContext2d(antialiasing = antialiased)
		ctx.render()
		img.image = image
	}

	abstract fun Context2d.render(): Unit
}

class VectorImage(app: Application) : BaseCanvas(app) {
	lateinit var d: Context2d.Drawable
	var targetWidth: Int? = null
	var targetHeight: Int? = null

	fun setVector(d: Context2d.Drawable, width: Int?, height: Int?) {
		this.d = d
		this.targetWidth = width
		this.targetHeight = height
		invalidate()
	}

	override fun Context2d.render() {
		val twidth = targetWidth
		val theight = targetHeight
		val sx = if (twidth != null) width.toDouble() / twidth.toDouble() else 1.0
		val sy = if (theight != null) height.toDouble() / theight.toDouble() else 1.0

		d.draw(this.withScaledRenderer(sx, sy))
		//d.draw(this)
	}
}

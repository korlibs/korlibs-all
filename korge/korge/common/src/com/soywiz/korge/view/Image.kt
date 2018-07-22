package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

class Image(
	var tex: Texture,
	var anchorX: Double = 0.0,
	var anchorY: Double = anchorX,
	views: Views,
	var hitShape: VectorPath? = null,
	var smoothing: Boolean = true
) : View(views) {
	private val sLeft get() = -tex.width * anchorX
	private val sTop get() = -tex.height * anchorY

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		// Precalculate points to avoid matrix multiplication per vertex on each frame
		ctx.batch.drawQuad(
			tex,
			x = -(tex.width * anchorX).toFloat(),
			y = -(tex.height * anchorY).toFloat(),
			m = m,
			filtering = smoothing,
			colorMul = globalColorMul,
			colorAdd = globalColorAdd,
			blendFactors = computedBlendMode.factors
		)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, tex.width, tex.height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		val sRight = sLeft + tex.width
		val sBottom = sTop + tex.height
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) && (hitShape?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)) this else null
	}

	override fun createInstance(): View = Image(tex, anchorX, anchorY, views, hitShape, smoothing)
}

class ImageBitmap(
	var bitmap: BitmapSlice<Bitmap>,
	var anchorX: Double = 0.0,
	var anchorY: Double = anchorX,
	views: Views,
	var hitShape: VectorPath? = null,
	var smoothing: Boolean = true
) : View(views) {
	private val sLeft get() = -bitmap.width * anchorX
	private val sTop get() = -bitmap.height * anchorY

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		// Precalculate points to avoid matrix multiplication per vertex on each frame
		ctx.batch.drawQuad(
			views.agBitmapTextureManager.getTexture(bitmap),
			x = -(bitmap.width * anchorX).toFloat(),
			y = -(bitmap.height * anchorY).toFloat(),
			m = m,
			filtering = smoothing,
			colorMul = globalColorMul,
			colorAdd = globalColorAdd,
			blendFactors = computedBlendMode.factors
		)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, bitmap.width, bitmap.height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		val sRight = sLeft + bitmap.width
		val sBottom = sTop + bitmap.height
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) && (hitShape?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)) this else null
	}

	override fun createInstance(): View = ImageBitmap(bitmap, anchorX, anchorY, views, hitShape, smoothing)
}

fun Views.image(tex: Texture, anchorX: Double = 0.0, anchorY: Double = anchorX) = Image(tex, anchorX, anchorY, this)
//fun Views.image(tex: TransformedTexture, anchorX: Double = 0.0, anchorY: Double = anchorX) = Image(tex.texture, anchorX, anchorY, this)

fun Views.imageBitmap(bmp: BitmapSlice<Bitmap>, anchorX: Double = 0.0, anchorY: Double = anchorX) = ImageBitmap(bmp, anchorX, anchorY, this)
fun Views.imageBitmap(bmp: Bitmap, anchorX: Double = 0.0, anchorY: Double = anchorX) = ImageBitmap(bmp.slice(), anchorX, anchorY, this)
//fun Views.image(tex: TransformedTexture, anchorX: Double = 0.0, anchorY: Double = anchorX) = Image(tex.texture, anchorX, anchorY, this)

fun Container.image(texture: Texture, anchorX: Double = 0.0, anchorY: Double = 0.0): Image =
	image(texture, anchorX, anchorY) { }

inline fun Container.image(
	texture: Texture,
	anchorX: Double = 0.0,
	anchorY: Double = 0.0,
	callback: Image.() -> Unit
): Image {
	val child = views.image(texture, anchorX, anchorY)
	this += child
	callback(child)
	return child
}


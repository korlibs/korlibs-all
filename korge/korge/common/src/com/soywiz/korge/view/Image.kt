package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

inline fun Container.image(
	texture: BmpSlice, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewsDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this).apply(callback)

inline fun Container.image(
	texture: Bitmap, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewsDslMarker Image.() -> Unit = {}
): Image = Image(texture, anchorX, anchorY).addTo(this).apply(callback)

class Image(
	var bitmap: BmpSlice,
	var anchorX: Double = 0.0,
	var anchorY: Double = anchorX,
	var hitShape: VectorPath? = null,
	var smoothing: Boolean = true
) : View() {
	constructor(
		bitmap: Bitmap,
		anchorX: Double = 0.0,
		anchorY: Double = anchorX,
		hitShape: VectorPath? = null,
		smoothing: Boolean = true
	) : this(bitmap.slice(), anchorX, anchorY, hitShape, smoothing)

	private val sLeft get() = -bitmap.width * anchorX
	private val sTop get() = -bitmap.height * anchorY

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		// Precalculate points to avoid matrix multiplication per vertex on each frame
		ctx.batch.drawQuad(
			ctx.getTex(bitmap),
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
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) && (hitShape?.containsPoint(
				globalToLocalX(
					x,
					y
				), globalToLocalY(x, y)
			) != false)
		) this else null
	}

	override fun createInstance(): View = Image(bitmap, anchorX, anchorY, hitShape, smoothing)
}

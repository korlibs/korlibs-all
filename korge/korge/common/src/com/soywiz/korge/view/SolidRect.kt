package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

class SolidRect(override var width: Double, override var height: Double, color: RGBAInt) : View() {
	init {
		this.colorMul = color
	}

	private var sLeft = 0.0
	private var sTop = 0.0

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		//println("%08X".format(color))
		ctx.batch.drawQuad(
			ctx.getTex(Bitmaps.white),
			x = 0f,
			y = 0f,
			width = width.toFloat(),
			height = height.toFloat(),
			m = m,
			filtering = false,
			colorMul = RGBA.multiply(colorMul, globalColorMul),
			colorAdd = globalColorAdd,
			blendFactors = computedBlendMode.factors
		)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, width, height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		return if (checkGlobalBounds(x, y, 0.0, 0.0, width, height)) this else null
	}

	override fun createInstance(): View = SolidRect(width, height, colorMul)
}

inline fun Views.solidRect(width: Number, height: Number, color: RGBAInt): SolidRect =
	SolidRect(width.toDouble(), height.toDouble(), color)
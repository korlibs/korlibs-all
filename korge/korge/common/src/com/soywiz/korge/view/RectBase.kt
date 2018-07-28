package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

open class RectBase(
	anchorX: Double = 0.0,
	anchorY: Double = anchorX,
	var hitShape: VectorPath? = null,
	var smoothing: Boolean = true
) : Container() {
	//abstract val width: Double
	//abstract val height: Double

	protected var baseBitmap: BmpSlice = Bitmaps.white; set(v) = run { field = v }.also { dirtyVertices = true }
	var anchorX: Double = anchorX; set (v) = run { field = v }.also { dirtyVertices = true }
	var anchorY: Double = anchorY; set(v) = run { field = v }.also { dirtyVertices = true }

	protected open val bwidth get() = width
	protected open val bheight get() = height

	protected open val sLeft get() = -bwidth * anchorX
	protected open val sTop get() = -bheight * anchorY

	private val vertices = TexturedVertexArray(4, TexturedVertexArray.QUAD_INDICES)

	private fun computeVertexIfRequired() {
		if (!dirtyVertices) return
		dirtyVertices = false
		val matrix = this.renderMatrix
		val x = sLeft
		val y = sTop
		val width = bwidth
		val height = bheight
		val colMul = globalColorMul
		val colAdd = globalColorAdd
		val bmp = baseBitmap
		vertices.select(0).xy(x, y, matrix).uv(bmp.tl_x, bmp.tl_y).cols(colMul, colAdd)
		vertices.select(1).xy(x + width, y, matrix).uv(bmp.tr_x, bmp.tr_y).cols(colMul, colAdd)
		vertices.select(2).xy(x + width, y + height, matrix).uv(bmp.br_x, bmp.br_y).cols(colMul, colAdd)
		vertices.select(3).xy(x, y + height, matrix).uv(bmp.bl_x, bmp.bl_y).cols(colMul, colAdd)
	}

	override fun render(ctx: RenderContext) {
		if (!visible) return
		super.render(ctx)
		computeVertexIfRequired()
		ctx.batch.drawVertices(vertices, ctx.getTex(baseBitmap).base, smoothing, computedBlendMode.factors)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, bwidth, bheight)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		val sRight = sLeft + bwidth
		val sBottom = sTop + bheight
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) &&
			(hitShape?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)
		) this else null
	}
}

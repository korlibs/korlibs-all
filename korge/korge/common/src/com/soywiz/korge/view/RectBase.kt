package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

open class RectBase(
	anchorX: Double = 0.0,
	anchorY: Double = anchorX,
	var hitShape: VectorPath? = null,
	var smoothing: Boolean = true
) : View() {
	//abstract val width: Double
	//abstract val height: Double

	protected var baseBitmap: BmpSlice = Bitmaps.white; set(v) = run { field = v }.also { dirtyVertices = true }
	var anchorX: Double = anchorX; set (v) = run { field = v }.also { dirtyVertices = true }
	var anchorY: Double = anchorY; set(v) = run { field = v }.also { dirtyVertices = true }

	protected open val bwidth get() = width
	protected open val bheight get() = height

	private val sLeft get() = -bwidth * anchorX
	private val sTop get() = -bheight * anchorY

	private val vertices = TexturedVertexArray(4, TexturedVertexArray.QUAD_INDICES)
	private val p0 = vertices.points[0]
	private val p1 = vertices.points[1]
	private val p2 = vertices.points[2]
	private val p3 = vertices.points[3]

	private fun computeVertexIfRequired() {
		if (!dirtyVertices) return
		dirtyVertices = false
		val matrix = this.globalMatrix // @TODO: Use matrix from reference instead
		val x = sLeft
		val y = sTop
		val width = bwidth
		val height = bheight
		val colMul = globalColorMul
		val colAdd = globalColorAdd
		val bmp = baseBitmap
		p0.setXY(x, y, matrix).setTXY(bmp.tl_x, bmp.tl_y).setCols(colMul, colAdd)
		p1.setXY(x + width, y, matrix).setTXY(bmp.tr_x, bmp.tr_y).setCols(colMul, colAdd)
		p2.setXY(x + width, y + height, matrix).setTXY(bmp.br_x, bmp.br_y).setCols(colMul, colAdd)
		p3.setXY(x, y + height, matrix).setTXY(bmp.bl_x, bmp.bl_y).setCols(colMul, colAdd)
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
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

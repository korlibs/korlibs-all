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

open class Image(
	bitmap: BmpSlice,
	anchorX: Double = 0.0,
	anchorY: Double = anchorX,
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

	var bitmap: BmpSlice = bitmap; set(v) = run { field = v }.also { dirtyVertices = true }
	var anchorX: Double = anchorX; set (v) = run { field = v }.also { dirtyVertices = true }
	var anchorY: Double = anchorY; set(v) = run { field = v }.also { dirtyVertices = true }

	private val sLeft get() = -bitmap.width * anchorX
	private val sTop get() = -bitmap.height * anchorY

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
		val width = width
		val height = height
		val colMul = globalColorMul
		val colAdd = globalColorAdd
		p0.setXY(x, y, matrix).setTXY(bitmap.tl_x, bitmap.tl_y).setCols(colMul, colAdd)
		p1.setXY(x + width, y, matrix).setTXY(bitmap.tr_x, bitmap.tr_y).setCols(colMul, colAdd)
		p2.setXY(x + width, y + height, matrix).setTXY(bitmap.br_x, bitmap.br_y).setCols(colMul, colAdd)
		p3.setXY(x, y + height, matrix).setTXY(bitmap.bl_x, bitmap.bl_y).setCols(colMul, colAdd)
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		computeVertexIfRequired()
		ctx.batch.drawVertices(vertices, ctx.getTex(bitmap).base, smoothing, computedBlendMode.factors)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, bitmap.width, bitmap.height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		val sRight = sLeft + bitmap.width
		val sBottom = sTop + bitmap.height
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) &&
			(hitShape?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)
		) this else null
	}

	override fun createInstance(): View = Image(bitmap, anchorX, anchorY, hitShape, smoothing)
}

inline fun <T : Image> T.anchor(ax: Number, ay: Number): T =
	this.apply { anchorX = ax.toDouble() }.apply { anchorY = ay.toDouble() }

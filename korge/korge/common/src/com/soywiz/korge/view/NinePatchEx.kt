package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

class NinePatchEx(
	views: Views,
	val ninePatch: NinePatchTex,
	override var width: Double,
	override var height: Double
) : View(views) {
	var smoothing = true

	private val bounds = RectangleInt()

	val NinePatchInfo.Segment.tex by Extra.PropertyThis<NinePatchInfo.Segment, Texture> {
		this@NinePatchEx.ninePatch.tex.slice(this.rect.toDouble())
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return

		// @TODO: Gracefully handle matrix scaling!

		bounds.setTo(0, 0, width.toInt(), height.toInt())

		ninePatch.info.computeScale(bounds) { segment, x, y, width, height ->
			ctx.batch.drawQuad(
				segment.tex,
				x.toFloat(), y.toFloat(),
				width.toFloat(), height.toFloat(),
				m = m,
				colorMul = colorMul,
				colorAdd = colorAdd,
				filtering = smoothing,
				blendFactors = blendMode.factors
			)
		}
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0.0, 0.0, width, height)
	}
}

class NinePatchTex(val tex: Texture, val info: NinePatchInfo) {
	val width get() = info.width
	val height get() = info.height
	constructor(views: Views, ninePatch: NinePatchBitmap32) : this(views.texture(ninePatch.content), ninePatch.info)
}

fun Views.ninePatchEx(
	tex: Texture, ninePatch: NinePatchInfo, width: Double = tex.width.toDouble(), height: Double = tex.height.toDouble()
) = NinePatchEx(this, NinePatchTex(tex, ninePatch), width, height)

fun Views.ninePatchEx(
	ninePatch: NinePatchBitmap32,
	width: Double = ninePatch.width.toDouble(), height: Double = ninePatch.height.toDouble()
): NinePatchEx = NinePatchEx(this, NinePatchTex(this, ninePatch), width, height)

fun Views.ninePatchEx(
	ninePatch: NinePatchTex,
	width: Double = ninePatch.width.toDouble(), height: Double = ninePatch.height.toDouble()
): NinePatchEx = NinePatchEx(this, ninePatch, width, height)

package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korma.*

class ScaleView(width: Int, height: Int, scale: Double = 2.0, var filtering: Boolean = false) :
	FixedSizeContainer() {
	init {
		this.width = width.toDouble()
		this.height = height.toDouble()
		this.scale = scale
	}

	//val once = Once()

	private fun super_render(ctx: RenderContext, m: Matrix2d) {
		super.render(ctx, m);
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		val iwidth = width.toInt()
		val iheight = height.toInt()

		ctx.renderToTexture(iwidth, iheight, renderToTexture = {
			//super.render(ctx, Matrix2d()) // @TODO: Bug with JTransc 0.6.6
			super_render(ctx, Matrix2d())
		}, use = { renderTexture ->
			ctx.batch.drawQuad(
				tex = renderTexture,
				x = 0f, y = 0f,
				width = iwidth.toFloat(),
				height = iheight.toFloat(),
				m = m,
				colorMul = colorMul,
				colorAdd = colorAdd,
				filtering = filtering
			)
			ctx.flush()
		})
	}
}

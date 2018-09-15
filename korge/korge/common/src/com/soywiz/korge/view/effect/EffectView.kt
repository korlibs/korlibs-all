package com.soywiz.korge.view.effect

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korma.*

open class EffectView : Container() {
	var filtering = true
	private val oldViewMatrix = Matrix4()
	var effectBorder = 0
	private val tempMat2d = Matrix2d()
	var vertex: VertexShader = BatchBuilder2D.VERTEX
		set(value) {
			field = value
			program = null
		}
	var fragment: FragmentShader = DEFAULT_FRAGMENT
		set(value) {
			field = value
			program = null
		}

	var program: Program? = null
	val uniforms = AG.UniformValues()

	companion object {
	    val DEFAULT_FRAGMENT = BatchBuilder2D.buildTextureLookupFragment(premultiplied = false)
	}

	override fun render(ctx: RenderContext) {
		val bounds = getLocalBounds()

		ctx.renderToTexture(bounds.width.toInt() + effectBorder * 2, bounds.height.toInt() + effectBorder * 2, renderToTexture = {
			tempMat2d.copyFrom(globalMatrixInv)
			tempMat2d.translate(effectBorder, effectBorder)
			ctx.batch.setViewMatrixTemp(tempMat2d, temp = oldViewMatrix) {
				super.render(ctx)
			}
		}) { texture ->
			// @TODO: Precompute vertices
			tempMat2d.copyFrom(globalMatrix)
			tempMat2d.pretranslate(-effectBorder, -effectBorder)
			if (program == null) program = Program(vertex, fragment)
			ctx.batch.setTemporalUniforms(this.uniforms) {
				ctx.batch.drawQuad(
					texture,
					m = tempMat2d,
					filtering = filtering,
					colorAdd = renderColorAdd,
					colorMulInt = renderColorMulInt,
					blendFactors = blendMode.factors,
					program = program
				)
			}
		}
	}
}

inline fun Container.effectView(callback: @ViewsDslMarker EffectView.() -> Unit = {}) =
	EffectView().addTo(this).apply(callback)

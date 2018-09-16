package com.soywiz.korge.view.effect

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korma.*

open class EffectView : Container() {
	var filtering = true
	private val oldViewMatrix = Matrix4()
	open var borderEffect = 0
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
	private val timeHolder = FloatArray(1)
	private val textureSizeHolder = FloatArray(2)
	val uniforms = AG.UniformValues(
		u_Time to timeHolder,
		u_TextureSize to textureSizeHolder
	)

	companion object {
		val u_Time = Uniform("time", VarType.Float1)
		val u_TextureSize = Uniform("effectTextureSize", VarType.Float2)
	    val DEFAULT_FRAGMENT = BatchBuilder2D.buildTextureLookupFragment(premultiplied = false)

		val Program.Builder.fragmentCoords01 get() = DefaultShaders.v_Tex["xy"]
		val Program.Builder.fragmentCoords get() = fragmentCoords01 * u_TextureSize
		fun Program.Builder.tex(coords: Operand) = texture2D(DefaultShaders.u_Tex, coords / u_TextureSize)
	}

	private var currentTimeMs = 0
		set(value) {
			field = value
			timeHolder[0] = (currentTimeMs.toDouble() / 1000.0).toFloat()
		}

	init {
		addUpdatable { ms ->
			currentTimeMs += ms
		}
	}

	override fun render(ctx: RenderContext) {
		val bounds = getLocalBounds()

		ctx.renderToTexture(bounds.width.toInt() + borderEffect * 2, bounds.height.toInt() + borderEffect * 2, renderToTexture = {
			tempMat2d.copyFrom(globalMatrixInv)
			tempMat2d.translate(borderEffect, borderEffect)
			ctx.batch.setViewMatrixTemp(tempMat2d, temp = oldViewMatrix) {
				super.render(ctx)
			}
		}) { texture ->
			// @TODO: Precompute vertices
			textureSizeHolder[0] = texture.base.width.toFloat()
			textureSizeHolder[1] = texture.base.height.toFloat()

			//println(textureSizeHolder.toList())
			tempMat2d.copyFrom(globalMatrix)
			tempMat2d.pretranslate(-borderEffect, -borderEffect)
			if (program == null) program = Program(vertex, fragment)
			//println("EffectUniforms: ${this.uniforms}")
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

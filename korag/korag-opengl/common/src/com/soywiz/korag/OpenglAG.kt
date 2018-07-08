package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.error.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.*

abstract class AGOpengl : AG() {
	abstract val gl: KmlGl

	override var devicePixelRatio: Double = 1.0

	//val queue = LinkedList<(gl: GL) -> Unit>()

	override fun createBuffer(kind: Buffer.Kind): Buffer = GlBuffer(kind)

	override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
		super.setViewport(x, y, width, height)
		checkErrors { gl.viewport(x, y, width, height) }
	}

	open fun setSwapInterval(value: Int) {
		//gl.swapInterval = 0
	}

	inner class GlRenderBuffer : RenderBuffer() {
		var cachedVersion = -1
		val wtex get() = tex as GlTexture

		val renderbufferDepth = KmlNativeBuffer(4)
		val framebuffer = KmlNativeBuffer(4)
		var oldViewport = IntArray(4)

		override fun start(width: Int, height: Int) {
			setSwapInterval(0)

			if (cachedVersion != contextVersion) {
				cachedVersion = contextVersion
				checkErrors { gl.genRenderbuffers(1, renderbufferDepth) }
				checkErrors { gl.genFramebuffers(1, framebuffer) }
			}

			getViewport(oldViewport)
			checkErrors { gl.bindTexture(gl.TEXTURE_2D, wtex.tex) }
			checkErrors { gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR) }
			checkErrors { gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR) }
			checkErrors { gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, null) }
			checkErrors { gl.bindTexture(gl.TEXTURE_2D, 0) }

			checkErrors { gl.bindRenderbuffer(gl.RENDERBUFFER, renderbufferDepth.getInt(0)) }
			checkErrors { gl.renderbufferStorage(gl.RENDERBUFFER, gl.DEPTH_COMPONENT16, width, height) }

			checkErrors { gl.bindFramebuffer(gl.FRAMEBUFFER, framebuffer.getInt(0)) }
			checkErrors {
				gl.framebufferTexture2D(
					gl.FRAMEBUFFER,
					gl.COLOR_ATTACHMENT0,
					gl.TEXTURE_2D,
					wtex.tex,
					0
				)
			}
			checkErrors {
				gl.framebufferRenderbuffer(
					gl.FRAMEBUFFER,
					gl.DEPTH_ATTACHMENT,
					gl.RENDERBUFFER,
					renderbufferDepth.getInt(0)
				)
			}
			setViewport(0, 0, width, height)
		}

		override fun end() {
			//checkErrors { gl.Flush() }
			checkErrors { gl.bindTexture(gl.TEXTURE_2D, 0) }
			checkErrors { gl.bindRenderbuffer(gl.RENDERBUFFER, 0) }
			checkErrors { gl.bindFramebuffer(gl.FRAMEBUFFER, 0) }
			setViewport(oldViewport)
		}

		override fun close() {
			checkErrors { gl.deleteFramebuffers(1, framebuffer) }
			checkErrors { gl.deleteRenderbuffers(1, renderbufferDepth) }
			framebuffer.setInt(0, 0)
			renderbufferDepth.setInt(0, 0)
		}
	}

	override fun createRenderBuffer(): RenderBuffer = GlRenderBuffer()

	private fun BlendEquation.toGl() = when (this) {
		BlendEquation.ADD -> gl.FUNC_ADD
		BlendEquation.SUBTRACT -> gl.FUNC_SUBTRACT
		BlendEquation.REVERSE_SUBTRACT -> gl.FUNC_REVERSE_SUBTRACT
	}

	private fun BlendFactor.toGl() = when (this) {
		BlendFactor.DESTINATION_ALPHA -> gl.DST_ALPHA
		BlendFactor.DESTINATION_COLOR -> gl.DST_COLOR
		BlendFactor.ONE -> gl.ONE
		BlendFactor.ONE_MINUS_DESTINATION_ALPHA -> gl.ONE_MINUS_DST_ALPHA
		BlendFactor.ONE_MINUS_DESTINATION_COLOR -> gl.ONE_MINUS_DST_COLOR
		BlendFactor.ONE_MINUS_SOURCE_ALPHA -> gl.ONE_MINUS_SRC_ALPHA
		BlendFactor.ONE_MINUS_SOURCE_COLOR -> gl.ONE_MINUS_SRC_COLOR
		BlendFactor.SOURCE_ALPHA -> gl.SRC_ALPHA
		BlendFactor.SOURCE_COLOR -> gl.SRC_COLOR
		BlendFactor.ZERO -> gl.ZERO
	}

	fun TriangleFace.toGl() = when (this) {
		TriangleFace.FRONT -> gl.FRONT
		TriangleFace.BACK -> gl.BACK
		TriangleFace.FRONT_AND_BACK -> gl.FRONT_AND_BACK
		TriangleFace.NONE -> gl.FRONT
	}

	fun CompareMode.toGl() = when (this) {
		CompareMode.ALWAYS -> gl.ALWAYS
		CompareMode.EQUAL -> gl.EQUAL
		CompareMode.GREATER -> gl.GREATER
		CompareMode.GREATER_EQUAL -> gl.GEQUAL
		CompareMode.LESS -> gl.LESS
		CompareMode.LESS_EQUAL -> gl.LEQUAL
		CompareMode.NEVER -> gl.NEVER
		CompareMode.NOT_EQUAL -> gl.NOTEQUAL
	}

	fun StencilOp.toGl() = when (this) {
		StencilOp.DECREMENT_SATURATE -> gl.DECR
		StencilOp.DECREMENT_WRAP -> gl.DECR_WRAP
		StencilOp.INCREMENT_SATURATE -> gl.INCR
		StencilOp.INCREMENT_WRAP -> gl.INCR_WRAP
		StencilOp.INVERT -> gl.INVERT
		StencilOp.KEEP -> gl.KEEP
		StencilOp.SET -> gl.REPLACE
		StencilOp.ZERO -> gl.ZERO
	}

	val tempBuffer1 = KmlNativeBuffer(4)
	val tempBuffer16 = KmlNativeBuffer(4 * 16)

	override fun draw(
		vertices: Buffer,
		program: Program,
		type: DrawType,
		vertexLayout: VertexLayout,
		vertexCount: Int,
		indices: Buffer?,
		offset: Int,
		blending: Blending,
		uniforms: Map<Uniform, Any>,
		stencil: StencilState,
		colorMask: ColorMaskState,
		renderState: RenderState,
		scissor: Scissor?
	) {
		val mustFreeIndices = indices == null
		val aindices = indices ?: createIndexBuffer((0 until vertexCount).map(Int::toShort).toShortArray())

		if (scissor != null) {
			gl.enable(gl.SCISSOR_TEST)
			gl.scissor(scissor.x, backHeight - scissor.y - scissor.height, scissor.width, scissor.height)
		} else {
			gl.disable(gl.SCISSOR_TEST)
		}

		checkBuffers(vertices, aindices)
		val glProgram = getProgram(program)
		(vertices as GlBuffer).bind(gl)
		(aindices as GlBuffer).bind(gl)
		glProgram.use()

		val totalSize = vertexLayout.totalSize
		for (n in vertexLayout.attributePositions.indices) {
			val att = vertexLayout.attributes[n]
			if (att.active) {
				val off = vertexLayout.attributePositions[n]
				val loc = checkErrors { gl.getAttribLocation(glProgram.id, att.name).toInt() }
				val glElementType = att.type.glElementType
				val elementCount = att.type.elementCount
				if (loc >= 0) {
					checkErrors { gl.enableVertexAttribArray(loc) }
					checkErrors {
						gl.vertexAttribPointer(
							loc,
							elementCount,
							glElementType,
							att.normalized,
							totalSize,
							off
							//off.toLong()
						)
					}
				}
			}
		}
		var textureUnit = 0
		for ((uniform, value) in uniforms) {
			val location = checkErrors { gl.getUniformLocation(glProgram.id, uniform.name) }
			when (uniform.type) {
				VarType.TextureUnit -> {
					val unit = value as TextureUnit
					checkErrors { gl.activeTexture(gl.TEXTURE0 + textureUnit) }
					val tex = (unit.texture as GlTexture?)
					tex?.bindEnsuring()
					tex?.setFilter(unit.linear)
					checkErrors { gl.uniform1i(location, textureUnit) }
					textureUnit++
				}
				VarType.Mat4 -> {
					checkErrors {
						gl.uniformMatrix4fv(
							location,
							1,
							false,
							tempBuffer16.setFloats(0, (value as Matrix4).data, 0, 16)
						)
					}
				}
				VarType.Float1 -> {
					checkErrors { gl.uniform1f(location, (value as Number).toFloat()) }
				}
				VarType.Float2 -> {
					val fa = value as FloatArray
					checkErrors { gl.uniform2f(location, fa[0], fa[1]) }
				}
				VarType.Float3 -> {
					val fa = value as FloatArray
					checkErrors { gl.uniform3f(location, fa[0], fa[1], fa[2]) }
				}
				VarType.Float4 -> {
					val fa = value as FloatArray
					checkErrors { gl.uniform4f(location, fa[0], fa[1], fa[2], fa[3]) }
				}
				else -> invalidOp("Don't know how to set uniform ${uniform.type}")
			}
		}

		if (blending.enabled) {
			checkErrors { gl.enable(gl.BLEND) }
			checkErrors { gl.blendEquationSeparate(blending.eqRGB.toGl(), blending.eqA.toGl()) }
			checkErrors {
				gl.blendFuncSeparate(
					blending.srcRGB.toGl(),
					blending.dstRGB.toGl(),
					blending.srcA.toGl(),
					blending.dstA.toGl()
				)
			}
		} else {
			checkErrors { gl.disable(gl.BLEND) }
		}

		gl.disable(gl.CULL_FACE)
		gl.frontFace(gl.CW)

		gl.depthMask(renderState.depthMask)

		gl.depthRangef(renderState.depthNear, renderState.depthFar)

		gl.lineWidth(renderState.lineWidth)

		if (renderState.depthFunc != CompareMode.ALWAYS) {
			checkErrors { gl.enable(gl.DEPTH_TEST) }
			checkErrors { gl.depthFunc(renderState.depthFunc.toGl()) }
		} else {
			checkErrors { gl.disable(gl.DEPTH_TEST) }
		}

		checkErrors { gl.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha) }

		if (stencil.enabled) {
			checkErrors { gl.enable(gl.STENCIL_TEST) }
			checkErrors { gl.stencilFunc(stencil.compareMode.toGl(), stencil.referenceValue, stencil.readMask) }
			checkErrors {
				gl.stencilOp(
					stencil.actionOnDepthFail.toGl(),
					stencil.actionOnDepthPassStencilFail.toGl(),
					stencil.actionOnBothPass.toGl()
				)
			}
			checkErrors { gl.stencilMask(stencil.writeMask) }
		} else {
			checkErrors { gl.disable(gl.STENCIL_TEST) }
			checkErrors { gl.stencilMask(0) }
		}

		//checkErrors { gl.drawElements(type.glDrawMode, vertexCount, gl.UNSIGNED_SHORT, offset.toLong()) }
		checkErrors { gl.drawElements(type.glDrawMode, vertexCount, gl.UNSIGNED_SHORT, offset) }

		checkErrors { gl.activeTexture(gl.TEXTURE0) }
		for (att in vertexLayout.attributes.filter { it.active }) {
			val loc = checkErrors { gl.getAttribLocation(glProgram.id, att.name).toInt() }
			if (loc >= 0) {
				checkErrors { gl.disableVertexAttribArray(loc) }
			}
		}

		if (mustFreeIndices) aindices.close()
	}

	val DrawType.glDrawMode: Int
		get() = when (this) {
			DrawType.POINTS -> gl.POINTS
			DrawType.LINE_STRIP -> gl.LINE_STRIP
			DrawType.LINE_LOOP -> gl.LINE_LOOP
			DrawType.LINES -> gl.LINES
			DrawType.TRIANGLE_STRIP -> gl.TRIANGLE_STRIP
			DrawType.TRIANGLE_FAN -> gl.TRIANGLE_FAN
			DrawType.TRIANGLES -> gl.TRIANGLES
		}

	val VarType.glElementType: Int
		get() = when (this.kind) {
			VarKind.BYTE -> gl.BYTE
			VarKind.UNSIGNED_BYTE -> gl.UNSIGNED_BYTE
			VarKind.SHORT -> gl.SHORT
			VarKind.UNSIGNED_SHORT -> gl.UNSIGNED_SHORT
			VarKind.INT -> gl.UNSIGNED_INT
			VarKind.FLOAT -> gl.FLOAT
		}

	private val programs = HashMap<Program, GlProgram>()
	fun getProgram(program: Program): GlProgram = programs.getOrPut(program) { GlProgram(gl, program) }

	inner class GlProgram(val gl: KmlGl, val program: Program) : Closeable {
		var cachedVersion = -1
		var id: Int = 0
		var fragmentShaderId: Int = 0
		var vertexShaderId: Int = 0

		private fun String.replaceVersion(version: Int) = this.replace("#version 100", "#version $version")

		private fun ensure() {
			if (cachedVersion != contextVersion) {
				cachedVersion = contextVersion
				id = checkErrors { gl.createProgram() }

				val glslVersionString = gl.getString(gl.SHADING_LANGUAGE_VERSION)
				val glslVersionInt = glslVersionString.replace(".", "").trim().toIntOrNull() ?: 100

				println("GL_SHADING_LANGUAGE_VERSION: $glslVersionInt : $glslVersionString")

				fragmentShaderId = createShader(
					gl.FRAGMENT_SHADER,
					program.fragment.toNewGlslString(gles = false, version = glslVersionInt)
				)
				vertexShaderId = createShader(
					gl.VERTEX_SHADER,
					program.vertex.toNewGlslString(gles = false, version = glslVersionInt)
				)
				checkErrors { gl.attachShader(id, fragmentShaderId) }
				checkErrors { gl.attachShader(id, vertexShaderId) }
				checkErrors { gl.linkProgram(id) }
				tempBuffer1.setInt(0, 0)
				checkErrors { gl.getProgramiv(id, gl.LINK_STATUS, tempBuffer1) }
			}
		}

		fun createShader(type: Int, str: String): Int {
			val shaderId = checkErrors { gl.createShader(type) }

			checkErrors { gl.shaderSource(shaderId, str) }
			checkErrors { gl.compileShader(shaderId) }

			val out = gl.getShaderiv(shaderId, gl.COMPILE_STATUS)
			if (out != gl.TRUE) {
				val error = gl.getShaderInfoLog(shaderId)
				Console.error(str)
				throw RuntimeException("Error Compiling Shader : $error")
			}
			return shaderId
		}

		fun use() {
			ensure()
			checkErrors { gl.useProgram(id) }
		}

		fun unuse() {
			ensure()
			checkErrors { gl.useProgram(0) }
		}

		override fun close() {
			checkErrors { gl.deleteShader(fragmentShaderId) }
			checkErrors { gl.deleteShader(vertexShaderId) }
			checkErrors { gl.deleteProgram(id) }
		}
	}

	override fun clear(
		color: Int,
		depth: Float,
		stencil: Int,
		clearColor: Boolean,
		clearDepth: Boolean,
		clearStencil: Boolean
	) {
		//println("CLEAR: $color, $depth")
		var bits = 0
		checkErrors { gl.disable(gl.SCISSOR_TEST) }
		if (clearColor) {
			bits = bits or gl.COLOR_BUFFER_BIT
			checkErrors { gl.clearColor(RGBA.getRf(color), RGBA.getGf(color), RGBA.getBf(color), RGBA.getAf(color)) }
		}
		if (clearDepth) {
			bits = bits or gl.DEPTH_BUFFER_BIT
			checkErrors { gl.clearDepthf(depth) }
		}
		if (clearStencil) {
			bits = bits or gl.STENCIL_BUFFER_BIT
			checkErrors { gl.stencilMask(-1) }
			checkErrors { gl.clearStencil(stencil) }
		}
		checkErrors { gl.clear(bits) }
	}

	override fun createTexture(premultiplied: Boolean): Texture = GlTexture(this.gl, premultiplied)

	inner class GlBuffer(kind: Buffer.Kind) : Buffer(kind) {
		var cachedVersion = -1
		private var id = -1
		val glKind = if (kind == Buffer.Kind.INDEX) gl.ELEMENT_ARRAY_BUFFER else gl.ARRAY_BUFFER

		override fun afterSetMem() {
		}

		override fun close() {
			kmlNativeBuffer(4) { buffer ->
				buffer.setInt(0, this.id)
				checkErrors { gl.deleteBuffers(1, buffer) }
			}
			id = -1
		}

		fun getGlId(gl: KmlGl): Int {
			if (cachedVersion != contextVersion) {
				cachedVersion = contextVersion
				id = -1
			}
			if (id < 0) {
				id = kmlNativeBuffer(4) {
					checkErrors { gl.genBuffers(1, it) }
					it.getInt(0)
				}
			}
			if (dirty) {
				_bind(gl, id)
				if (mem != null) {
					checkErrors { gl.bufferData(glKind, memLength, mem!!, gl.STATIC_DRAW) }
				}
			}
			return id
		}

		fun _bind(gl: KmlGl, id: Int) {
			checkErrors { gl.bindBuffer(glKind, id) }
		}

		fun bind(gl: KmlGl) {
			_bind(gl, getGlId(gl))
		}
	}

	inner class GlTexture(val gl: KmlGl, override val premultiplied: Boolean) : Texture() {
		var cachedVersion = -1
		val texIds = KmlNativeBuffer(4)

		val tex: Int
			get() {
				if (cachedVersion != contextVersion) {
					cachedVersion = contextVersion
					invalidate()
					checkErrors { gl.genTextures(1, texIds) }
				}
				return texIds.getInt(0)
			}

		fun createBufferForBitmap(bmp: Bitmap?): KmlNativeBuffer? {
			return when (bmp) {
				null -> null
				is NativeImage -> unsupported("Should not call createBufferForBitmap with a NativeImage")
				is Bitmap8 -> {
					val mem = KmlNativeBuffer(bmp.area)
					arraycopy(bmp.data, 0, mem.arrayByte, 0, bmp.area)
					@Suppress("USELESS_CAST")
					return mem
				}
				is Bitmap32 -> {
					val abmp: Bitmap32 =
						if (premultiplied) bmp.premultipliedIfRequired() else bmp.depremultipliedIfRequired()
					//println("BMP: Bitmap32")
					//val abmp: Bitmap32 = bmp
					val mem = KmlNativeBuffer(abmp.area * 4)
					arraycopy(abmp.data, 0, mem.arrayInt, 0, abmp.area)
					@Suppress("USELESS_CAST")
					return mem
				}
				else -> unsupported()
			}
		}

		override fun actualSyncUpload(source: BitmapSourceBase, bmp: Bitmap?, requestMipmaps: Boolean) {
			val bytesPerPixel = if (source.rgba) 4 else 1
			val type = if (source.rgba) {
				//if (source is NativeImage) gl.BGRA else gl.RGBA
				gl.RGBA
			} else {
				gl.LUMINANCE
			}

			if (bmp is NativeImage) {
				gl.texImage2D(gl.TEXTURE_2D, 0, type, type, gl.UNSIGNED_BYTE, bmp)
			} else {
				val buffer = createBufferForBitmap(bmp)
				if (buffer != null) {
					checkErrors {
						gl.texImage2D(
							gl.TEXTURE_2D, 0, type,
							source.width, source.height,
							0, type, gl.UNSIGNED_BYTE, buffer
						)
					}
				}
				//println(buffer)
			}

			this.mipmaps = false

			if (requestMipmaps) {
				//println(" - mipmaps")
				this.mipmaps = true
				bind()
				setFilter(true)
				setWrapST()
				checkErrors { gl.generateMipmap(gl.TEXTURE_2D) }
			} else {
				//println(" - nomipmaps")
			}
		}

		override fun bind(): Unit = checkErrors { gl.bindTexture(gl.TEXTURE_2D, tex) }
		override fun unbind(): Unit = checkErrors { gl.bindTexture(gl.TEXTURE_2D, 0) }

		private var closed = false
		override fun close(): Unit = checkErrors {
			super.close()
			if (!closed) {
				closed = true
				gl.deleteTextures(1, texIds)
			}
		}

		fun setFilter(linear: Boolean) {
			val minFilter = if (this.mipmaps) {
				if (linear) gl.LINEAR_MIPMAP_NEAREST else gl.NEAREST_MIPMAP_NEAREST
			} else {
				if (linear) gl.LINEAR else gl.NEAREST
			}
			val magFilter = if (linear) gl.LINEAR else gl.NEAREST

			setWrapST()
			setMinMag(minFilter, magFilter)
		}

		private fun setWrapST() {
			checkErrors { gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE) }
			checkErrors { gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE) }
		}

		private fun setMinMag(min: Int, mag: Int) {
			checkErrors { gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, min) }
			checkErrors { gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, mag) }
		}
	}

	inline fun <T> checkErrors(callback: () -> T): T = callback()

	//inline fun <T> checkErrors(callback: () -> T): T {
	//	val res = callback()
	//	if (checkErrors) {
	//		val error = gl.getError()
	//		if (error != gl.NO_ERROR) {
	//			Console.error("OpenGL error: $error")
	//			//System.err.println(Throwable().stackTrace)
	//			Throwable().printStackTrace()
	//			//throw RuntimeException("OpenGL error: $error")
	//		}
	//	}
	//	return res
	//}


	override fun readColor(bitmap: Bitmap32) {
		kmlNativeBuffer(bitmap.area * 4) { buffer ->
			gl.readPixels(
				0, 0, bitmap.width, bitmap.height,
				gl.RGBA, gl.UNSIGNED_BYTE, buffer
			)
			buffer.getAlignedArrayInt32(0, bitmap.data, 0, bitmap.area)
		}
	}

	override fun readDepth(width: Int, height: Int, out: FloatArray) {
		val area = width * height
		kmlNativeBuffer(area * 4) { buffer ->
			gl.readPixels(
				0, 0, width, height, gl.DEPTH_COMPONENT, gl.FLOAT,
				buffer
			)
			buffer.getAlignedArrayFloat32(0, out, 0, area)
		}
	}
}

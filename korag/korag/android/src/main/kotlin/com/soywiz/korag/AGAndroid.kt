package com.soywiz.korag

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.soywiz.kmem.FastMemory
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.toGlSlString
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.android.KorioAndroidContext
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.error.unsupported
import com.soywiz.korio.util.Once
import com.soywiz.korma.Matrix4
import java.io.Closeable
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

actual object AGFactoryFactory {
	actual fun create(): AGFactory = AGFactoryAndroid
}

object AGFactoryAndroid : AGFactory {
	override val supportsNativeFrame: Boolean = false
	override fun create(): AG = AGAndroid()
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		TODO()
	}
}

private typealias GL = GLES20
private typealias gl = GLES20

class AGAndroid : AG() {
	val ag = this
	val glv = GLSurfaceView(KorioAndroidContext)
	override val nativeComponent: Any = glv

	override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
		super.setViewport(x, y, width, height)
		gl.glViewport(x, y, width, height)
	}

	init {
		//glv.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
		glv.setEGLContextClientVersion(2)
		glv.setRenderer(object : GLSurfaceView.Renderer {
			val onReadyOnce = Once()

			private fun initializeOnce() {
				onReadyOnce {
					ready()
				}
			}

			override fun onDrawFrame(gl1: GL10) {
				//println("Android.onDrawFrame")
				initializeOnce()
				//if (DEBUG_AGANDROID) println("Android.onDrawFrame... " + Thread.currentThread())
				onRender(ag)
				//gl = gl1 as GLES20
			}

			override fun onSurfaceChanged(gl1: GL10, width: Int, height: Int) {
				setViewport(0, 0, width, height)
				initializeOnce()
				//resized()
				onRender(ag)
			}

			override fun onSurfaceCreated(gl1: GL10, p1: EGLConfig) {
				initializeOnce()
				//gl = gl1 as GLES20
				onRender(ag)
			}
		})
		glv.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
	}

	override fun repaint() {
		glv.requestRender()
	}
//
	//override fun resized() {
	//	if (initialized) {
	//		gl.glViewport(0, 0, backWidth, backHeight)
	//	}
	//}

	override fun clear(color: Int, depth: Float, stencil: Int, clearColor: Boolean, clearDepth: Boolean, clearStencil: Boolean) {
		var bits = 0
		if (clearColor) bits = bits or GL.GL_COLOR_BUFFER_BIT
		if (clearDepth) bits = bits or GL.GL_DEPTH_BUFFER_BIT
		if (clearStencil) bits = bits or GL.GL_STENCIL_BUFFER_BIT
		gl.glClearColor(RGBA.getRf(color), RGBA.getGf(color), RGBA.getBf(color), RGBA.getAf(color))
		gl.glClearDepthf(depth)
		gl.glClearStencil(stencil)
		gl.glClear(bits)
		//println("Android.glClear")
	}

	override fun createBuffer(kind: Buffer.Kind): Buffer = AndroidBuffer(kind)

	private fun BlendEquation.toGl() = when (this) {
		BlendEquation.ADD -> GL.GL_FUNC_ADD
		BlendEquation.SUBTRACT -> GL.GL_FUNC_SUBTRACT
		BlendEquation.REVERSE_SUBTRACT -> GL.GL_FUNC_REVERSE_SUBTRACT
	}

	private fun BlendFactor.toGl() = when (this) {
		BlendFactor.DESTINATION_ALPHA -> GL.GL_DST_ALPHA
		BlendFactor.DESTINATION_COLOR -> GL.GL_DST_COLOR
		BlendFactor.ONE -> GL.GL_ONE
		BlendFactor.ONE_MINUS_DESTINATION_ALPHA -> GL.GL_ONE_MINUS_DST_ALPHA
		BlendFactor.ONE_MINUS_DESTINATION_COLOR -> GL.GL_ONE_MINUS_DST_COLOR
		BlendFactor.ONE_MINUS_SOURCE_ALPHA -> GL.GL_ONE_MINUS_SRC_ALPHA
		BlendFactor.ONE_MINUS_SOURCE_COLOR -> GL.GL_ONE_MINUS_SRC_COLOR
		BlendFactor.SOURCE_ALPHA -> GL.GL_SRC_ALPHA
		BlendFactor.SOURCE_COLOR -> GL.GL_SRC_COLOR
		BlendFactor.ZERO -> GL.GL_ZERO
	}

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
		renderState: RenderState
	) {
		val mustFreeIndices = indices == null
		val aindices = indices ?: createIndexBuffer((0 until vertexCount).map(Int::toShort).toShortArray())

		checkBuffers(vertices, aindices)
		val glProgram = getProgram(program)
		(vertices as AndroidBuffer).bind()
		(aindices as AndroidBuffer).bind()
		glProgram.use()

		val totalSize = vertexLayout.totalSize
		for (n in vertexLayout.attributePositions.indices) {
			val att = vertexLayout.attributes[n]
			if (att.active) {
				val off = vertexLayout.attributePositions[n]
				val loc = gl.glGetAttribLocation(glProgram.id, att.name).toInt()
				val glElementType = att.type.glElementType
				val elementCount = att.type.elementCount
				if (loc >= 0) {
					gl.glEnableVertexAttribArray(loc)
					gl.glVertexAttribPointer(loc, elementCount, glElementType, att.normalized, totalSize, off)
				}
			}
		}
		var textureUnit = 0
		for ((uniform, value) in uniforms) {
			val location = gl.glGetUniformLocation(glProgram.id, uniform.name)
			when (uniform.type) {
				VarType.TextureUnit -> {
					val unit = value as TextureUnit
					gl.glActiveTexture(GL.GL_TEXTURE0 + textureUnit)
					val tex = (unit.texture as AndroidTexture?)
					tex?.bindEnsuring()
					tex?.setFilter(unit.linear)
					gl.glUniform1i(location, textureUnit)
					textureUnit++
				}
				VarType.Mat4 -> {
					gl.glUniformMatrix4fv(location, 1, false, (value as Matrix4).data, 0)
				}
				VarType.Float1 -> {
					gl.glUniform1f(location, (value as Number).toFloat())
				}
				else -> invalidOp("Don't know how to set uniform ${uniform.type}")
			}
		}

		if (blending.disabled) {
			gl.glDisable(GL.GL_BLEND)
		} else {
			gl.glEnable(GL.GL_BLEND)
			gl.glBlendEquationSeparate(blending.eqRGB.toGl(), blending.eqA.toGl())
			gl.glBlendFuncSeparate(blending.srcRGB.toGl(), blending.dstRGB.toGl(), blending.srcA.toGl(), blending.dstA.toGl())
		}

		gl.glDrawElements(type.glDrawMode, vertexCount, GL.GL_UNSIGNED_SHORT, offset)

		gl.glActiveTexture(GL.GL_TEXTURE0)
		for (att in vertexLayout.attributes.filter { it.active }) {
			val loc = gl.glGetAttribLocation(glProgram.id, att.name).toInt()
			if (loc >= 0) {
				gl.glDisableVertexAttribArray(loc)
			}
		}

		if (mustFreeIndices) aindices.close()
	}

	val DrawType.glDrawMode: Int
		get() = when (this) {
			DrawType.POINTS -> GL.GL_POINTS
			DrawType.LINE_STRIP -> GL.GL_LINE_STRIP
			DrawType.LINE_LOOP -> GL.GL_LINE_LOOP
			DrawType.LINES -> GL.GL_LINES
			DrawType.TRIANGLE_STRIP -> GL.GL_TRIANGLE_STRIP
			DrawType.TRIANGLE_FAN -> GL.GL_TRIANGLE_FAN
			DrawType.TRIANGLES -> GL.GL_TRIANGLES
		}

	val VarType.glElementType: Int
		get() = when (this.kind) {
			VarKind.BYTE -> GL.GL_BYTE
			VarKind.UNSIGNED_BYTE -> GL.GL_UNSIGNED_BYTE
			VarKind.SHORT -> GL.GL_SHORT
			VarKind.UNSIGNED_SHORT -> GL.GL_UNSIGNED_SHORT
			VarKind.INT -> GL.GL_INT
			VarKind.FLOAT -> GL.GL_FLOAT
		}

	private val programs = hashMapOf<String, AndroidProgram>()
	fun getProgram(program: Program): AndroidProgram = programs.getOrPut(program.name) { AndroidProgram(program) }

	class AndroidProgram(val program: Program) : Closeable {
		val id = gl.glCreateProgram()
		val fragmentShaderId = createShader(GL.GL_FRAGMENT_SHADER, program.fragment.toGlSlString())
		val vertexShaderId = createShader(GL.GL_VERTEX_SHADER, program.vertex.toGlSlString())

		init {
			gl.glAttachShader(id, fragmentShaderId)
			gl.glAttachShader(id, vertexShaderId)
			gl.glLinkProgram(id)
			val out = IntArray(1)
			gl.glGetProgramiv(id, GL.GL_LINK_STATUS, out, 0)
			if (out[0] != GL.GL_TRUE) {
				val msg = gl.glGetProgramInfoLog(id)
				throw RuntimeException("Error Linking Program : '$msg' programId=$id")
			}
		}

		fun createShader(type: Int, str: String): Int {
			val shaderId = gl.glCreateShader(type)
			gl.glShaderSource(shaderId, str)
			gl.glCompileShader(shaderId)

			val out = IntArray(1)
			gl.glGetShaderiv(shaderId, GL.GL_COMPILE_STATUS, out, 0)
			if (out[0] != GL.GL_TRUE) {
				System.err.println(str)
				throw RuntimeException("Error Compiling Shader : " + gl.glGetShaderInfoLog(shaderId))
			}
			return shaderId
		}

		fun use() {
			gl.glUseProgram(id)
		}

		fun unuse() {
			gl.glUseProgram(0)
		}

		override fun close() {
			gl.glDeleteShader(fragmentShaderId)
			gl.glDeleteShader(vertexShaderId)
			gl.glDeleteProgram(id)
		}
	}

	inner class AndroidBuffer(kind: Buffer.Kind) : Buffer(kind) {
		private var id = -1
		val glKind = if (kind == Buffer.Kind.INDEX) GL.GL_ELEMENT_ARRAY_BUFFER else GL.GL_ARRAY_BUFFER

		override fun afterSetMem() {
		}

		override fun close() {
			val deleteId = id
			gl.glDeleteBuffers(1, intArrayOf(deleteId), 0)
			id = -1
		}

		fun getGlId(): Int {
			if (id < 0) {
				val out = IntArray(1)
				gl.glGenBuffers(1, out, 0)
				id = out[0]
			}
			if (dirty) {
				_bind(id)
				if (mem != null) {
					val mem2: FastMemory? = mem
					val bb = mem2?.buffer?.buffer
					if (bb != null) {
						val pos = bb.position()
						bb.position(memOffset)
						//println("Setting buffer($kind): ${mem.byteBufferOrNull}")
						gl.glBufferData(glKind, memLength, bb, GL.GL_STATIC_DRAW)
						bb.position(pos)
					}
				}
			}
			return id
		}

		fun _bind(id: Int) {
			gl.glBindBuffer(glKind, id)
		}

		fun bind() {
			_bind(getGlId())
		}
	}

	inner class AndroidTexture : Texture() {
		val texIds = IntArray(1)

		init {
			gl.glGenTextures(1, texIds, 0)
		}

		val tex = texIds[0]

		fun createBufferForBitmap(bmp: Bitmap?): ByteBuffer {
			return when (bmp) {
				null -> ByteBuffer.allocateDirect(0)
				is NativeImage -> createBufferForBitmap(bmp.toBmp32())
				is Bitmap8 -> {
					val mem = FastMemory.alloc(bmp.area)
					mem.setAlignedArrayInt8(0, bmp.data, 0, bmp.area)
					return mem.buffer.buffer
				}
				is Bitmap32 -> {
					val mem = FastMemory.alloc(bmp.area * 4)
					mem.setAlignedArrayInt32(0, bmp.data, 0, bmp.area)
					return mem.buffer.buffer
				}
				else -> unsupported()
			}
		}

		override fun actualSyncUpload(source: BitmapSourceBase, bmp: Bitmap?, requestMipmaps: Boolean) {
			val type = if (source.rgba) GL.GL_RGBA else GL.GL_LUMINANCE
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, type, source.width, source.height, 0, type, GL.GL_UNSIGNED_BYTE, createBufferForBitmap(bmp))

			this.mipmaps = false

			if (requestMipmaps) {
				bind()
				setFilter(true)
				setWrapST()
				//glm["generateMipmap"](gl["TEXTURE_2D"]); this.mipmaps = true
			}
		}

		override fun bind(): Unit = run { gl.glBindTexture(GL.GL_TEXTURE_2D, tex) }
		override fun unbind(): Unit = run { gl.glBindTexture(GL.GL_TEXTURE_2D, 0) }

		override fun close(): Unit = run { gl.glDeleteTextures(1, texIds, 0) }

		fun setFilter(linear: Boolean) {
			val minFilter = if (this.mipmaps) {
				if (linear) GL.GL_LINEAR_MIPMAP_NEAREST else GL.GL_NEAREST_MIPMAP_NEAREST
			} else {
				if (linear) GL.GL_LINEAR else GL.GL_NEAREST
			}
			val magFilter = if (linear) GL.GL_LINEAR else GL.GL_NEAREST

			setWrapST()
			setMinMag(minFilter, magFilter)
		}

		private fun setWrapST() {
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE)
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE)
		}

		private fun setMinMag(min: Int, mag: Int) {
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, min)
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, mag)
		}
	}
}
package com.soywiz.korag.log

import com.soywiz.kmem.FastMemory
import com.soywiz.korag.AG
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.lang.printStackTrace

open class LogAG(
	width: Int = 640,
	height: Int = 480
) : AG() {
	val log = arrayListOf<String>()
	override val nativeComponent: Any = Any()

	init {
		ready()
	}

	private fun log(str: String) {
		this.log += str
		//println(str)
	}

	fun getLogAsString(): String = log.joinToString("\n")

	override fun clear(color: Int, depth: Float, stencil: Int, clearColor: Boolean, clearDepth: Boolean, clearStencil: Boolean) = log("clear($color, $depth, $stencil, $clearColor, $clearDepth, $clearStencil)")
	override var backWidth: Int = width; set(value) = run { field = value; log("backWidth = $value") }
	override var backHeight: Int = height; set(value) = run { field = value; log("backHeight = $value") }

	override fun repaint() = log("repaint()")

	override fun resized() {
		log("resized()")
		onResized(Unit)
	}

	override fun dispose() = log("dispose()")

	inner class LogTexture(val id: Int, override val premultiplied: Boolean) : Texture() {
		override fun uploadedSource() {
			log("$this.uploadedBitmap($source, ${source.width}, ${source.height})")
		}

		override fun close() = log("$this.close()")
		override fun toString(): String = "Texture[$id]"
	}

	inner class LogBuffer(val id: Int, kind: Kind) : Buffer(kind) {
		val logmem: FastMemory? get() = mem
		val logmemOffset get() = memOffset
		val logmemLength get() = memLength
		override fun afterSetMem() = log("$this.afterSetMem(mem[${mem!!.size}])")
		override fun close() = log("$this.close()")
		override fun toString(): String = "Buffer[$id]"
	}

	inner class LogRenderBuffer(val id: Int) : RenderBuffer() {
		override fun start(width: Int, height: Int) = log("$this.start($width, $height)")
		override fun end() = log("$this.end()")
		override fun close() = log("$this.close()")
		override fun toString(): String = "RenderBuffer[$id]"
	}

	private var textureId = 0
	private var bufferId = 0
	private var renderBufferId = 0

	override fun createTexture(premultiplied: Boolean): Texture = LogTexture(textureId++, premultiplied).apply { log("createTexture():$id") }

	override fun createBuffer(kind: Buffer.Kind): Buffer = LogBuffer(bufferId++, kind).apply { log("createBuffer($kind):$id") }
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
		try {
			log("draw(vertices=$vertices, indices=$indices, program=$program, type=$type, vertexLayout=$vertexLayout, vertexCount=$vertexCount, offset=$offset, blending=$blending, uniforms=$uniforms, stencil=$stencil, colorMask=$colorMask)")

			val missingUniforms = program.uniforms - uniforms.keys
			val extraUniforms = uniforms.keys - program.uniforms
			val missingAttributes = vertexLayout.attributes.toSet() - program.attributes
			val extraAttributes = program.attributes - vertexLayout.attributes.toSet()

			if (missingUniforms.isNotEmpty()) log("::draw.ERROR.Missing:$missingUniforms")
			if (extraUniforms.isNotEmpty()) log("::draw.ERROR.Unexpected:$extraUniforms")

			if (missingAttributes.isNotEmpty()) log("::draw.ERROR.Missing:$missingAttributes")
			if (extraAttributes.isNotEmpty()) log("::draw.ERROR.Unexpected:$extraAttributes")

			val vertexBuffer = vertices as LogBuffer
			val vertexMem = vertexBuffer.logmem!!
			val vertexMemOffset = vertexBuffer.logmemOffset
			val indexMem = (indices as LogBuffer).logmem
			val _indices = (offset until offset + vertexCount).map { indexMem!!.getAlignedInt16(it) }
			log("::draw.indices=$_indices")
			for (index in _indices.sorted().distinct()) {
				val os = index * vertexLayout.totalSize
				val attributes = arrayListOf<String>()
				for ((attribute, pos) in vertexLayout.attributes.zip(vertexLayout.attributePositions)) {
					val o = os + pos + vertexMemOffset

					val info = when (attribute.type) {
						VarType.Int1 -> "int(" + vertexMem.getInt32(o + 0) + ")"
						VarType.Float1 -> "float(" + vertexMem.getFloat32(o + 0) + ")"
						VarType.Float2 -> "vec2(" + vertexMem.getFloat32(o + 0) + "," + vertexMem.getFloat32(o + 4) + ")"
						VarType.Float3 -> "vec3(" + vertexMem.getFloat32(o + 0) + "," + vertexMem.getFloat32(o + 4) + "," + vertexMem.getFloat32(o + 8) + ")"
						VarType.Byte4 -> "byte4(" + vertexMem.getInt32(o + 0) + ")"
						else -> "Unsupported(${attribute.type})"
					}

					attributes += attribute.name + "[" + info + "]"
				}
				log("::draw.vertex[$index]: " + attributes.joinToString(", "))
			}
		} catch (e: Throwable) {
			log("ERROR: ${e.message}")
			e.printStackTrace()
		}
	}

	override fun disposeTemporalPerFrameStuff() = log("disposeTemporalPerFrameStuff()")
	override fun createRenderBuffer(): RenderBuffer = LogRenderBuffer(renderBufferId++).apply { log("createRenderBuffer():$id") }
	override fun flipInternal() = log("flipInternal()")
	override fun readColor(bitmap: Bitmap32) = log("$this.readBitmap($bitmap)")
	override fun readDepth(width: Int, height: Int, out: FloatArray) = log("$this.readDepth($width, $height, $out)")
}
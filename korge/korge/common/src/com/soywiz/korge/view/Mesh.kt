package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*

class Mesh(
	var texture: BmpSlice? = null,
	var vertices: Float32Buffer = Float32BufferAlloc(0),
	var uvs: Float32Buffer = Float32BufferAlloc(0),
	var indices: Uint16Buffer = Uint16BufferAlloc(0),
	var drawMode: DrawModes = DrawModes.Triangles
) : View() {
	enum class DrawModes { Triangles, TriangleStrip }

	val textureNN get() = texture ?: Bitmaps.white
	var dirty: Int = 0
	var indexDirty: Int = 0

	override fun render(ctx: RenderContext) {
		println("@TODO: Mesh.render!") // @TODO: Mesh.render()
	}
}
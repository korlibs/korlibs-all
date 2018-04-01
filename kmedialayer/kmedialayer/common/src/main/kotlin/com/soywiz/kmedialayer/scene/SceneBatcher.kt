package com.soywiz.kmedialayer.scene

import com.soywiz.kmedialayer.*
import com.soywiz.kmedialayer.scene.geom.*

// http://en.esotericsoftware.com/forum/Premultiply-Alpha-3132
class SceneBatcher(val gl: KmlGl, initialWidth: Int, initialHeight: Int) {
	val QUADS = 1024
	val vertices = KmlNativeBuffer(4 * QUADS * 4 * 4)
	val indices = KmlNativeBuffer(2 * QUADS * 6)
	var qcount = 0
	var vcount = 0
	var vpos = 0
	var ipos = 0
	private var currentTex: KmlGlTex? = null
	val ortho = KmlGlUtil.ortho(initialWidth, initialHeight)
	val program = gl.createProgram(
		vertex = """
            uniform mat4 uprojection;
            attribute vec2 aPos;
            attribute vec2 aTex;
            attribute float aAlpha;
            varying vec2 vTex;
            varying float vAlpha;
            void main() {
                gl_Position = uprojection * vec4(aPos, 0.0, 1.0);
                vTex = aTex;
                vAlpha = aAlpha;
            }
        """,
		fragment = """
            uniform sampler2D utex;
            varying vec2 vTex;
            varying float vAlpha;

            void main(void) {
                gl_FragColor = texture2D(utex, vTex);
                gl_FragColor.a *= vAlpha; // TODO: Depending on premultiplication we should multiply the whole color
            }
        """
	)
	val layout = program.layout {
		float("aPos", 2)
		float("aTex", 2)
		float("aAlpha", 1)
	}
	val vertexBuffer = gl.createArrayBuffer()
	val indexBuffer = gl.createElementArrayBuffer()

	private fun reset() {
		currentTex = null
		qcount = 0
		vcount = 0
		vpos = 0
		ipos = 0
	}

	private fun addVertex(x: Float, y: Float, tx: Float, ty: Float, alpha: Float) {
		vcount++
		vertices.setFloat(vpos++, x)
		vertices.setFloat(vpos++, y)
		vertices.setFloat(vpos++, tx)
		vertices.setFloat(vpos++, ty)
		vertices.setFloat(vpos++, alpha)
	}

	private fun addIndex(index: Int) {
		indices.setShort(ipos++, index.toShort())
	}

	fun addQuad(
		x0: Float,
		y0: Float,
		x1: Float,
		y1: Float,
		x2: Float,
		y2: Float,
		x3: Float,
		y3: Float,
		tex: SceneTexture,
		alpha: Float
	) {
		if (currentTex != tex.tex) {
			flush()
		}
		currentTex = tex.tex
		val vstart = vcount

		if (qcount >= QUADS - 1) {
			flush()
		}
		qcount++
		addVertex(x0, y0, tex.fleft, tex.ftop, alpha)
		addVertex(x1, y1, tex.fright, tex.ftop, alpha)
		addVertex(x2, y2, tex.fleft, tex.fbottom, alpha)
		addVertex(x3, y3, tex.fright, tex.fbottom, alpha)
		addIndex(vstart + 0)
		addIndex(vstart + 1)
		addIndex(vstart + 2)
		addIndex(vstart + 1)
		addIndex(vstart + 2)
		addIndex(vstart + 3)
	}

	fun addQuad(quad: Quad, tex: SceneTexture, alpha: Float) {
		addQuad(
			quad.p0.x.toFloat(), quad.p0.y.toFloat(),
			quad.p1.x.toFloat(), quad.p1.y.toFloat(),
			quad.p2.x.toFloat(), quad.p2.y.toFloat(),
			quad.p3.x.toFloat(), quad.p3.y.toFloat(),
			tex, alpha
		)
	}

	fun addQuad(left: Float, top: Float, right: Float, bottom: Float, tex: SceneTexture, alpha: Float) {
		addQuad(
			left, top,
			right, top,
			left, bottom,
			right, bottom,
			tex,
			alpha
		)
	}

	fun flush() {
		if (ipos > 0) {
			renderBatch()
			reset()
		}
	}

	private fun renderBatch() {
		gl.enable(gl.BLEND)
		gl.blendFuncSeparate(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA, gl.ONE, gl.ONE_MINUS_SRC_ALPHA)
		vertexBuffer.setData(vertices, vpos * 4)
		indexBuffer.setData(indices, ipos * 2)
		layout.drawElements(vertexBuffer, indexBuffer, gl.TRIANGLES, ipos, gl.UNSIGNED_SHORT) {
			currentTex?.let { uniformTex(program.getUniformLocation("utex"), it, unit = 0) }
			uniformMatrix4fv(program.getUniformLocation("uprojection"), 1, false, ortho)
		}
	}

	fun dispose() {

	}
}

class Quad {
	val p0 = Point()
	val p1 = Point()
	val p2 = Point()
	val p3 = Point()

	fun set(gm: Matrix2d, sx: Double, sy: Double, width: Double, height: Double) {
		p0.setToTransform(gm, sx, sy)
		p1.setToTransform(gm, sx + width, sy)
		p2.setToTransform(gm, sx, sy + height)
		p3.setToTransform(gm, sx + width, sy + height)
	}
}

class QuadWithTexture(val quad: Quad, val tex: SceneTexture)
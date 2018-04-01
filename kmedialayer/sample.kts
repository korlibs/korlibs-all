#!/usr/bin/env kscript

// Works with: https://github.com/holgerbrandl/kscript

@file:KotlinOpts("-J-XstartOnFirstThread")
@file:DependsOn("com.soywiz:kmedialayer:0.0.2")
@file:MavenRepository("soywiz-bintray", "https://dl.bintray.com/soywiz/soywiz/")

import com.soywiz.kmedialayer.*

Kml.application(
	WindowConfig(
		width = 640,
		height = 480,
		title = "KMediaLayerSample"
	), object : KMLWindowListener() {
		lateinit var program: KmlGlProgram
		lateinit var layout: KmlGlVertexLayout
		lateinit var vertexBuffer: KmlGlBuffer
		lateinit var indexBuffer: KmlGlBuffer
		lateinit var tex: KmlGlTex
		val ortho = KmlGlUtil.ortho(640, 480)

		override suspend fun init(gl: KmlGl) = gl.run {
			program = createProgram(
				vertex = """
                            uniform mat4 uprojection;
                            attribute vec2 aPos;
                            attribute vec2 aTex;
                            varying vec2 vTex;
                            void main() {
                                gl_Position = uprojection * vec4(aPos, 0.0, 1.0);
                                vTex = aTex;
                            }
                        """,
				fragment = """
                            uniform sampler2D utex;
                            varying vec2 vTex;

                            void main(void) {
                                //gl_FragColor = vec4(0.8, 0.3, 0.4, 1.0);
                                //texture2D(utex, vTex);
                                gl_FragColor = texture2D(utex, vTex);
                            }
                        """
			)
			layout = program.layout {
				float("aPos", 2)
				float("aTex", 2)
			}
			vertexBuffer = createArrayBuffer()
			indexBuffer = createElementArrayBuffer()

			tex = createKmlTexture()

			//Kml.launch {
			//Kml.delay(1000)
			//val miniPNG =
			//    unhex("89504E470D0A1A0A0000000D494844520000002000000020080300000044A48AC600000006504C5445FFFFFF243B601B86B322000000414944415478DADDD2310A00200C0441F7FF9F160463E51D9888E03682994AD38A6324E792B03A0373B215717F19A8C80B3E1206BCFF4D0F8833BDD546D8872EAD03B22200D3EE8E10050000000049454E44AE426082")
			//println("Decoding image...")
			//val miniImage = Kml.decodeImage(miniPNG)
			val miniImage = Kml.decodeImage("mini.png")
			tex.upload(miniImage).apply { smooth = false }
			println(miniImage)
			//}

			println("Created texture")
			//tex = createKmlTexture().upload(2, 2, intArrayOf(0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt()).toIntBuffer())
		}

		var n = 0
		override fun render(gl: KmlGl) = gl.run {
			val w = tex.width.toFloat() * 4
			val h = tex.height.toFloat() * 4
			vertexBuffer.setData(
				kmlFloatBufferOf(
					0f, 0f, 0f, 0f,
					w, 0f, 1f, 0f,
					0f, h, 0f, 1f,
					w, h, 1f, 1f
				)
			)
			indexBuffer.setData(
				kmlShortBufferOf(
					0, 1, 2,
					1, 2, 3
				)
			)
			n++
			clearColor(.5f, .55f, .6f, 1f)
			clear(COLOR_BUFFER_BIT)

			enable(BLEND)
			blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA)

			//println(ortho)
			//layout.drawArrays(vertexBuffer, TRIANGLE_STRIP, 0, 4) {
			layout.drawElements(vertexBuffer, indexBuffer, TRIANGLES, 6) {
				uniformTex(program.getUniformLocation("utex"), tex, unit = 0)
				uniformMatrix4fv(program.getUniformLocation("uprojection"), 1, false, ortho)
			}
		}

		override fun resized(width: Int, height: Int) {
			KmlGlUtil.ortho(width, height, out = ortho)
			println("Resized: $width, $height")
		}

		override fun keyUpdate(keyCode: Int, pressed: Boolean) {
			println("keyUpdate($keyCode, $pressed)")
		}

		override fun gamepadUpdate(button: Int, pressed: Boolean, ratio: Double) {
			//println("gamepadUpdate($button, $pressed, $ratio)")
		}

		override fun mouseUpdate(x: Int, y: Int, buttons: Int) {
			//println("mouseUpdate($x, $y, $buttons)")
		}
	}
)

fun unhex(c: Char): Int = when (c) {
	in '0'..'9' -> 0 + (c - '0')
	in 'a'..'f' -> 10 + (c - 'a')
	in 'A'..'F' -> 10 + (c - 'A')
	else -> throw RuntimeException("Illegal HEX character $c")
}

fun unhex(str: String): ByteArray {
	val out = ByteArray(str.length / 2)
	var m = 0
	for (n in 0 until out.size) {
		out[n] = ((unhex(str[m++]) shl 4) or unhex(str[m++])).toByte()
	}
	return out
}

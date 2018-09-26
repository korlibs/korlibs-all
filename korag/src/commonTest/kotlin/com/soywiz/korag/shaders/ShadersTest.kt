package com.soywiz.korag.shaders

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.util.*
import kotlin.test.*

class ShadersTest {
	@Test
	fun testGlslGeneration() {
		val vs = VertexShader {
			IF(true.lit) {
				DefaultShaders.t_Temp0 setTo 1.lit * 2.lit
			} ELSE {
				DefaultShaders.t_Temp0 setTo 3.lit * 4.lit
			}
		}

		// @TODO: Optimizer phase!
		assertEquals(
			Indenter {
				line("void main()") {
					line("vec4 temp0;")
					line("if (true)") {
						line("temp0 = (1 * 2);")
					}
					line("else") {
						line("temp0 = (3 * 4);")
					}
				}
			}.toString(),
			vs.toGlSlString(gles = false)
		)
	}
}
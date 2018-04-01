package com.soywiz.korag.shaders

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import kotlin.test.*

class ShadersTest {
	@Test
	fun testGlslGeneration() {
		val vs = VertexShader {
			IF(true.lit) {
				SET(DefaultShaders.t_Temp1, 1.lit * 2.lit)
			} ELSE {
				SET(DefaultShaders.t_Temp1, 3.lit * 4.lit)
			}
		}

		// @TODO: Optimizer phase!
		assertEquals(
			"void main() {vec4 temp0;{if (true) {temp0 = (1*2);} else {temp0 = (3*4);}}}",
			vs.toGlSlString(gles = false).trim()
		)
	}
}
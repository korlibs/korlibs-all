package com.soywiz.korge.ext.swf

import com.soywiz.korim.vector.*
import com.soywiz.korio.serialization.yaml.*
import org.junit.*

class SWFExportConfigTest {
	@Test
	fun name() {
		val config = Yaml.decodeToType<SWFExportConfig>(
			"""
				|mipmaps: false
				|rasterizerMethod: X2
			""".trimMargin()
		)

		Assert.assertEquals(
			SWFExportConfig(mipmaps = false, rasterizerMethod = Context2d.ShapeRasterizerMethod.X2),
			config
		)
	}
}

package com.soywiz.korim.bitmap

import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class NinePatchBitmap32Test {
	@Test
	fun name() = suspendTest {
		val ninePatch = ResourcesVfs["bubble-chat.9.png"].readNinePatch(defaultImageFormats)
		assertEquals(
			listOf(false to (0 until 91), true to (91 until 158), false to (158 until 202)),
			ninePatch.info.xsegments.map { it.scaled to it.range }
		)
		assertEquals(
			listOf(false to (0 until 57), true to (57 until 109), false to (109 until 202)),
			ninePatch.info.ysegments.map { it.scaled to it.range }
		)
		assertEquals(135, ninePatch.info.fixedWidth)
		assertEquals(150, ninePatch.info.fixedHeight)

		assertEquals(
			"""
				IRectangle(x=0, y=0, width=512, height=256):
				 - IRectangle(x=0, y=0, width=91, height=57):0,0,91,57
				 - IRectangle(x=91, y=0, width=67, height=57):91,0,377,57
				 - IRectangle(x=158, y=0, width=44, height=57):468,0,44,57
				 - IRectangle(x=0, y=57, width=91, height=52):0,57,91,106
				 - IRectangle(x=91, y=57, width=67, height=52):91,57,377,106
				 - IRectangle(x=158, y=57, width=44, height=52):468,57,44,106
				 - IRectangle(x=0, y=109, width=91, height=93):0,163,91,93
				 - IRectangle(x=91, y=109, width=67, height=93):91,163,377,93
				 - IRectangle(x=158, y=109, width=44, height=93):468,163,44,93
				IRectangle(x=0, y=0, width=256, height=512):
				 - IRectangle(x=0, y=0, width=91, height=57):0,0,91,57
				 - IRectangle(x=91, y=0, width=67, height=57):91,0,121,57
				 - IRectangle(x=158, y=0, width=44, height=57):212,0,44,57
				 - IRectangle(x=0, y=57, width=91, height=52):0,57,91,362
				 - IRectangle(x=91, y=57, width=67, height=52):91,57,121,362
				 - IRectangle(x=158, y=57, width=44, height=52):212,57,44,362
				 - IRectangle(x=0, y=109, width=91, height=93):0,419,91,93
				 - IRectangle(x=91, y=109, width=67, height=93):91,419,121,93
				 - IRectangle(x=158, y=109, width=44, height=93):212,419,44,93
				IRectangle(x=0, y=0, width=100, height=100):
				 - IRectangle(x=0, y=0, width=91, height=57):0,0,45,28
				 - IRectangle(x=91, y=0, width=67, height=57):45,0,33,28
				 - IRectangle(x=158, y=0, width=44, height=57):78,0,21,28
				 - IRectangle(x=0, y=57, width=91, height=52):0,28,45,25
				 - IRectangle(x=91, y=57, width=67, height=52):45,28,33,25
				 - IRectangle(x=158, y=57, width=44, height=52):78,28,21,25
				 - IRectangle(x=0, y=109, width=91, height=93):0,53,45,46
				 - IRectangle(x=91, y=109, width=67, height=93):45,53,33,46
				 - IRectangle(x=158, y=109, width=44, height=93):78,53,21,46
				IRectangle(x=0, y=0, width=0, height=0):
				 - IRectangle(x=0, y=0, width=91, height=57):0,0,0,0
				 - IRectangle(x=91, y=0, width=67, height=57):0,0,0,0
				 - IRectangle(x=158, y=0, width=44, height=57):0,0,0,0
				 - IRectangle(x=0, y=57, width=91, height=52):0,0,0,0
				 - IRectangle(x=91, y=57, width=67, height=52):0,0,0,0
				 - IRectangle(x=158, y=57, width=44, height=52):0,0,0,0
				 - IRectangle(x=0, y=109, width=91, height=93):0,0,0,0
				 - IRectangle(x=91, y=109, width=67, height=93):0,0,0,0
				 - IRectangle(x=158, y=109, width=44, height=93):0,0,0,0
			""".trimIndent(),
			arrayListOf<String>().apply {
				val log = this
				for (rect in listOf(RectangleInt(0, 0, 512, 256), RectangleInt(0, 0, 256, 512), RectangleInt(0, 0, 100, 100), RectangleInt(0, 0, 0, 0))) {
					log += "$rect:"
					ninePatch.info.computeScale(rect) { seg, x, y, width, height ->
						log += " - ${seg.rect}:$x,$y,$width,$height"
					}
				}
			}.joinToString("\n")
		)

		//val bmp = NativeImage(512, 256)
		//val bmp = NativeImage(202, 202)

		//for (segment in ninePatch.segments.flatMap { it }) showImageAndWait(segment.bmp)

		//ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 202, 202))
		//ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 512, 202))
		//ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 32, 202))

		//showImageAndWait(ninePatch.rendered(512, 256))
		//showImageAndWait(ninePatch.rendered(256, 512))
		//showImageAndWait(ninePatch.rendered(100, 100))
		//showImageAndWait(ninePatch.rendered(32, 100))
		//bmp.writeTo("/tmp/file.tga".uniVfs, defaultImageFormats)
	}
}
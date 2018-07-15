package com.soywiz.korma.geom.clipper

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.clipper.Clipper
import kotlin.test.*

class ClipperTest {
	@Test
	fun name() {
		val clipper = DefaultClipper()
		val path1 = Path(Point2d(0, 0), Point2d(10, 0), Point2d(10, 10), Point2d(0, 10))
		val path2 = Path(Point2d(5 + 0, 0), Point2d(5 + 10, 0), Point2d(5 + 10, 10), Point2d(5 + 0, 10))
		val paths = Paths()

		clipper.addPath(path1, Clipper.PolyType.CLIP, true)
		clipper.addPath(path2, Clipper.PolyType.SUBJECT, true)
		clipper.execute(Clipper.ClipType.INTERSECTION, paths)

		assertEquals("[[(10, 10), (5, 10), (5, 0), (10, 0)]]", paths.toString())
		assertEquals("Rectangle(x=5, y=0, width=5, height=10)", paths.bounds.toString())
	}
}
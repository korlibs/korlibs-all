package com.soywiz.korma.geom.shape

import com.soywiz.korma.geom.*
import kotlin.test.*

class Shape2dTest {
	@Test
	fun name() {
		assertEquals(
			"Rectangle(x=5, y=0, width=5, height=10)",
			(Shape2d.Rectangle(0, 0, 10, 10) intersection Shape2d.Rectangle(5, 0, 10, 10)).toString()
		)

		assertEquals(
			"Complex(items=[Rectangle(x=10, y=0, width=5, height=10), Rectangle(x=0, y=0, width=5, height=10)])",
			(Shape2d.Rectangle(0, 0, 10, 10) xor Shape2d.Rectangle(5, 0, 10, 10)).toString()
		)
	}

	@Test
	fun extend() {
		assertEquals(
			"Rectangle(x=-10, y=-10, width=30, height=30)",
			(Shape2d.Rectangle(0, 0, 10, 10).extend(10.0)).toString()
		)
	}

	@Test
	fun vectorPathToShape2d() {
		val vp = VectorPath().apply { circle(0.0, 0.0, 100.0) }
		assertEquals(78, vp.toShape2d().paths.totalVertices)
	}

	@Test
	fun triangulate() {
		assertEquals(
			"[Triangle((0, 100), (100, 0), (100, 100)), Triangle((0, 100), (0, 0), (100, 0))]",
			Rectangle(0, 0, 100, 100).toShape().triangulate().toString()
		)
	}

	@Test
	fun pathFind() {
		assertEquals(
			"[(10, 10), (90, 90)]",
			Rectangle(0, 0, 100, 100).toShape().pathFind(Point2d(10, 10), Point2d(90, 90)).toString()
		)
		assertEquals(
			"[(10, 10), (100, 50), (120, 52)]",
			(Rectangle(0, 0, 100, 100).toShape() + Rectangle(100, 50, 50, 50).toShape()).pathFind(
				Point2d(10, 10),
				Point2d(120, 52)
			).toString()
		)
	}
}
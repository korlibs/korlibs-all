package com.soywiz.korma.geom.shape

import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.VectorPath
import org.junit.Test
import kotlin.test.assertEquals

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
			"[Triangle(Vector2(0, 100), Vector2(100, 0), Vector2(100, 100)), Triangle(Vector2(0, 100), Vector2(0, 0), Vector2(100, 0))]",
			Rectangle(0, 0, 100, 100).toShape().triangulate().toString()
		)
	}

	@Test
	fun pathFind() {
		assertEquals("[Vector2(10, 10), Vector2(90, 90)]", Rectangle(0, 0, 100, 100).toShape().pathFind(Point2d(10, 10), Point2d(90, 90)).toString())
		assertEquals("[Vector2(10, 10), Vector2(100, 50), Vector2(120, 52)]", (Rectangle(0, 0, 100, 100).toShape() + Rectangle(100, 50, 50, 50).toShape()).pathFind(Point2d(10, 10), Point2d(120, 52)).toString())
	}
}
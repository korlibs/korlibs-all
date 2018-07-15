package com.soywiz.korma.geom

import com.soywiz.korma.*
import kotlin.test.*

class AngleTest {
	fun testAngleTo() {
		assertEquals(Angle.fromDegrees(0.0), Point2d(0, 0).angleTo(Point2d(100, 0)))
		assertEquals(Angle.fromDegrees(90.0), Point2d(0, 0).angleTo(Point2d(0, 100)))
		assertEquals(Angle.fromDegrees(180.0), Point2d(0, 0).angleTo(Point2d(-100, 0)))
		assertEquals(Angle.fromDegrees(270.0), Point2d(0, 0).angleTo(Point2d(0, -100)))

		assertEquals(Angle.fromDegrees(0.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 100, 1000 + 0)))
		assertEquals(Angle.fromDegrees(90.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 0, 1000 + 100)))
		assertEquals(Angle.fromDegrees(180.0), Point2d(1000, 1000).angleTo(Point2d(1000 + -100, 1000 + 0)))
		assertEquals(Angle.fromDegrees(270.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 0, 1000 + -100)))
	}
}

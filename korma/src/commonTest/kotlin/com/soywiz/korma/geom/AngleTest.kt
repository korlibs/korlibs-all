package com.soywiz.korma.geom

import com.soywiz.korma.*
import kotlin.test.*

class AngleTest {
	@Test
	fun testAngleTo() {
		//assertEquals(Angle.fromDegrees(0.0), Point2d(0, 0).angleTo(Point2d(100, 0)))
		//assertEquals(Angle.fromDegrees(90.0), Point2d(0, 0).angleTo(Point2d(0, 100)))
		//assertEquals(Angle.fromDegrees(180.0), Point2d(0, 0).angleTo(Point2d(-100, 0)))
		//assertEquals(Angle.fromDegrees(270.0), Point2d(0, 0).angleTo(Point2d(0, -100)))
//
		//assertEquals(Angle.fromDegrees(0.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 100, 1000 + 0)))
		//assertEquals(Angle.fromDegrees(90.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 0, 1000 + 100)))
		//assertEquals(Angle.fromDegrees(180.0), Point2d(1000, 1000).angleTo(Point2d(1000 + -100, 1000 + 0)))
		//assertEquals(Angle.fromDegrees(270.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 0, 1000 + -100)))
	}

	@Test
	fun testAngleOps() {

		assertEquals(180.degrees, 90.degrees + 90.degrees)
		assertEquals(180.degrees, 90.degrees * 2)
		assertEquals(45.degrees, 90.degrees / 2)

		assertEquals(0.degrees, (360 * 2.0).degrees.normalized)
		assertEquals(0.0, (360 * 2.0).degrees.normalizedDegrees)
		assertEquals(0.0, (360 * 2.0).degrees.normalizedRadians)
		//assertEquals(90.degrees, 180.degrees - 90.degrees)
	}

	// @TODO: Required to avoid: java.lang.AssertionError: expected:<3.141592653589793> but was:<Angle(180.0)>
	fun assertEquals(a: Angle, b: Angle) {
		assertEquals(a.degrees, b.degrees)
	}
}

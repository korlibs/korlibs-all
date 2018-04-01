package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle
import kotlin.math.abs
import kotlin.math.sqrt

//(x0,y0) is start point; (x1,y1),(x2,y2) is control points; (x3,y3) is end point.
interface Bezier {
	fun getBounds(target: Rectangle = Rectangle()): Rectangle
	fun calc(t: Double, target: Point2d = Point2d()): Point2d

	class Quad(val p0: Point2d, val p1: Point2d, val p2: Point2d) : Bezier {
		override fun getBounds(target: Rectangle): Rectangle = quadBounds(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, target)
		override fun calc(t: Double, target: Point2d): Point2d = quadCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, t, target)

		// http://fontforge.github.io/bezier.html
		fun toCubic(): Cubic = Cubic(
			p0,
			p0 + (p1 - p0) * (2.0 / 3.0),
			p2 + (p1 - p2) * (2.0 / 3.0),
			p2
		)
	}

	class Cubic(val p0: Point2d, val p1: Point2d, val p2: Point2d, val p3: Point2d) : Bezier {
		override fun getBounds(target: Rectangle): Rectangle = cubicBounds(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, target)
		override fun calc(t: Double, target: Point2d): Point2d = cubicCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, t, target)
	}

	companion object {
		private val tvalues = DoubleArray(6)
		private val xvalues = DoubleArray(8)
		private val yvalues = DoubleArray(8)

		// http://fontforge.github.io/bezier.html
		//Any quadratic spline can be expressed as a cubic (where the cubic term is zero). The end points of the cubic will be the same as the quadratic's.
		//CP0 = QP0
		//CP3 = QP2
		//The two control points for the cubic are:
		//CP1 = QP0 + 2/3 *(QP1-QP0)
		//CP2 = QP2 + 2/3 *(QP1-QP2)
		inline fun <T> quadToCubic(
			x0: Double, y0: Double, xc: Double, yc: Double, x1: Double, y1: Double,
			bezier: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> T
		): T {
			return bezier(
				x0, y0,
				x0 + 2 / 3 * (xc - x0), y0 + 2 / 3 * (yc - y0),
				x1 + 2 / 3 * (xc - x1), y1 + 2 / 3 * (yc - y1),
				x1, y1
			)
		}

		fun quadBounds(x0: Double, y0: Double, xc: Double, yc: Double, x1: Double, y1: Double, target: Rectangle = Rectangle()): Rectangle {
			// @TODO: Make an optimized version!
			return quadToCubic(x0, y0, xc, yc, x1, y1) { x0, y0, x1, y1, x2, y2, x3, y3 -> cubicBounds(x0, y0, x1, y1, x2, y2, x3, y3, target) }
		}

		inline fun <T> quadCalc(x0: Double, y0: Double, xc: Double, yc: Double, x1: Double, y1: Double, t: Double, emit: (x: Double, y: Double) -> T): T {
			//return quadToCubic(x0, y0, xc, yc, x1, y1) { x0, y0, x1, y1, x2, y2, x3, y3 -> cubicCalc(x0, y0, x1, y1, x2, y2, x3, y3, t, emit) }
			val t1 = (1 - t)
			val a = t1 * t1
			val c = t * t
			val b = 2 * t1 * t
			return emit(
				a * x0 + b * xc + c * x1,
				a * y0 + b * yc + c * y1
			)
		}

		fun quadCalc(x0: Double, y0: Double, xc: Double, yc: Double, x1: Double, y1: Double, t: Double, target: Point2d = Point2d()): Point2d {
			return quadCalc(x0, y0, xc, yc, x1, y1, t) { x, y -> target.setTo(x, y) }
		}

		fun cubicBounds(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, target: Rectangle = Rectangle()): Rectangle {
			var j = 0
			var a: Double
			var b: Double
			var c: Double
			var b2ac: Double
			var sqrtb2ac: Double
			for (i in 0 until 2) {
				if (i == 0) {
					b = 6 * x0 - 12 * x1 + 6 * x2
					a = -3 * x0 + 9 * x1 - 9 * x2 + 3 * x3
					c = 3 * x1 - 3 * x0
				} else {
					b = 6 * y0 - 12 * y1 + 6 * y2
					a = -3 * y0 + 9 * y1 - 9 * y2 + 3 * y3
					c = 3 * y1 - 3 * y0
				}
				if (abs(a) < 1e-12) {
					if (abs(b) >= 1e-12) {
						val t = -c / b
						if (0 < t && t < 1) tvalues[j++] = t
					}
				} else {
					b2ac = b * b - 4 * c * a
					if (b2ac < 0) continue
					sqrtb2ac = sqrt(b2ac)
					val t1 = (-b + sqrtb2ac) / (2 * a)
					if (0 < t1 && t1 < 1) tvalues[j++] = t1
					val t2 = (-b - sqrtb2ac) / (2 * a)
					if (0 < t2 && t2 < 1) tvalues[j++] = t2
				}
			}

			while (j-- > 0) {
				val t = tvalues[j]
				val mt = 1 - t
				xvalues[j] = (mt * mt * mt * x0) + (3 * mt * mt * t * x1) + (3 * mt * t * t * x2) + (t * t * t * x3)
				yvalues[j] = (mt * mt * mt * y0) + (3 * mt * mt * t * y1) + (3 * mt * t * t * y2) + (t * t * t * y3)
			}

			xvalues[tvalues.size + 0] = x0
			xvalues[tvalues.size + 1] = x3
			yvalues[tvalues.size + 0] = y0
			yvalues[tvalues.size + 1] = y3

			return target.setBounds(xvalues.min() ?: 0.0, yvalues.min() ?: 0.0, xvalues.max() ?: 0.0, yvalues.max() ?: 0.0)
		}

		inline fun <T> cubicCalc(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, t: Double, emit: (x: Double, y: Double) -> T): T {
			val cx = 3.0 * (x1 - x0)
			val bx = 3.0 * (x2 - x1) - cx
			val ax = x3 - x0 - cx - bx

			val cy = 3.0 * (y1 - y0)
			val by = 3.0 * (y2 - y1) - cy
			val ay = y3 - y0 - cy - by

			val tSquared = t * t
			val tCubed = tSquared * t

			return emit(
				ax * tCubed + bx * tSquared + cx * t + x0,
				ay * tCubed + by * tSquared + cy * t + y0
			)
		}

		// http://stackoverflow.com/questions/7348009/y-coordinate-for-a-given-x-cubic-bezier
		fun cubicCalc(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, t: Double, target: Point2d = Point2d()): Point2d {
			return cubicCalc(x0, y0, x1, y1, x2, y2, x3, y3, t) { x, y -> target.setTo(x, y) }
		}
	}
}

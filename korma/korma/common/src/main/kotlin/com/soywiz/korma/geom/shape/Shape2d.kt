package com.soywiz.korma.geom.shape

import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.clipper.Path
import com.soywiz.korma.geom.clipper.Paths
import com.soywiz.korma.math.Math
import com.soywiz.korma.numeric.niceStr
import kotlin.math.PI

abstract class Shape2d {
	abstract val paths: Paths
	abstract val closed: Boolean
	abstract val area: Double
	open fun containsPoint(x: Double, y: Double) = false

	object Empty : Shape2d() {
		override val paths: Paths = Paths()
		override val closed: Boolean = false
		override val area: Double = 0.0
		override fun containsPoint(x: Double, y: Double) = false
	}

	data class Line(val x0: Double, val y0: Double, val x1: Double, val y1: Double) : Shape2d() {
		override val paths get() = Paths(Path(listOf(Point2d(x0, y0), Point2d(x1, y1))))
		override val closed: Boolean = false
		override val area: Double get() = 0.0
		override fun containsPoint(x: Double, y: Double) = false
	}

	data class Circle(val x: Double, val y: Double, val radius: Double, val totalPoints: Int = 32) : Shape2d() {
		override val paths get() = Paths(Path((0 until totalPoints).map {
			Point2d(
				x + Angle.cos01(it.toDouble() / totalPoints.toDouble()) * radius,
				y + Angle.sin01(it.toDouble() / totalPoints.toDouble()) * radius
			)
		}))
		override val closed: Boolean = true
		override val area: Double get() = PI * radius * radius
		override fun containsPoint(x: Double, y: Double) = Math.hypot(this.x - x, this.y - y) < radius
	}

	data class Rectangle(val x: Double, val y: Double, val width: Double, val height: Double) : Shape2d() {
		constructor(x: Int, y: Int, width: Int, height: Int) : this(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

		val left: Double get() = x
		val top: Double get() = y
		val right: Double get() = x + width
		val bottom: Double get() = y + height
		override val paths = Paths(Path(listOf(
			Point2d(x, y),
			Point2d(x + width, y),
			Point2d(x + width, y + height),
			Point2d(x, y + height)
		)))
		override val closed: Boolean = true
		override val area: Double get() = width * height
		override fun containsPoint(x: Double, y: Double) = (x in this.left..this.right) && (y in this.top..this.bottom)
		override fun toString(): String = "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
	}

	data class Polygon(val points: List<Point2d>) : Shape2d() {
		override val paths = Paths(Path(points))
		override val closed: Boolean = true
		override val area: Double by lazy { this.triangulate().sumByDouble { it.area } }
		override fun containsPoint(x: Double, y: Double): Boolean = this.points.containsPoint(x, y)
	}

	data class Poyline(val points: List<Point2d>) : Shape2d() {
		override val paths = Paths(Path(points))
		override val closed: Boolean = false
		override val area: Double get() = 0.0
		override fun containsPoint(x: Double, y: Double) = false
	}

	data class Complex(val items: List<Shape2d>) : Shape2d() {
		override val paths get() = Paths(items.flatMap { it.paths })
		override val closed: Boolean = false
		override val area: Double by lazy { this.triangulate().sumByDouble { it.area } }
		override fun containsPoint(x: Double, y: Double): Boolean = this.getAllPoints().containsPoint(x, y)
	}
}

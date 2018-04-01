package com.soywiz.korma.geom.shape

import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.VectorPath
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.clipper.*
import com.soywiz.korma.geom.triangle.Sweep
import com.soywiz.korma.geom.triangle.SweepContext
import com.soywiz.korma.geom.triangle.Triangle
import com.soywiz.korma.math.Math

fun Path.toShape2d(): Shape2d {
	if (this.size == 4) {
		for (n in 0 until 4) {
			val tl = this[(n + 0) % 4]
			val tr = this[(n + 1) % 4]
			val br = this[(n + 2) % 4]
			val bl = this[(n + 3) % 4]

			if ((tl.x == bl.x) && (tr.x == br.x) && (tl.y == tr.y) && (bl.y == br.y)) {
				val xmin = Math.min(tl.x, tr.x)
				val xmax = Math.max(tl.x, tr.x)
				val ymin = Math.min(tl.y, bl.y)
				val ymax = Math.max(tl.y, bl.y)
				//println("($xmin,$ymin)-($xmax-$ymax) : $tl,$tr,$br,$bl")
				return Shape2d.Rectangle(xmin, ymin, xmax - xmin, ymax - ymin)
			}
		}
	}
	// @TODO: Try to detect rectangle
	return Shape2d.Polygon(this)
}

fun Paths.toShape2d(): Shape2d {
	return when (size) {
		0 -> Shape2d.Empty
		1 -> first().toShape2d()
		else -> Shape2d.Complex(this.map(Path::toShape2d))
	}
}

val Shape2d.bounds: Rectangle get() = paths.bounds

fun Rectangle.toShape() = Shape2d.Rectangle(x, y, width, height)

fun Shape2d.clipperOp(other: Shape2d, op: Clipper.ClipType): Shape2d {
	val clipper = DefaultClipper()
	val solution = Paths()
	clipper.addPaths(this.paths, Clipper.PolyType.CLIP, other.closed)
	clipper.addPaths(other.paths, Clipper.PolyType.SUBJECT, other.closed)
	clipper.execute(op, solution)
	return solution.toShape2d()
}

infix fun Shape2d.collidesWith(other: Shape2d): Boolean = this.clipperOp(other, Clipper.ClipType.INTERSECTION) != Shape2d.Empty
infix fun Shape2d.intersection(other: Shape2d): Shape2d = this.clipperOp(other, Clipper.ClipType.INTERSECTION)
infix fun Shape2d.union(other: Shape2d): Shape2d = this.clipperOp(other, Clipper.ClipType.UNION)
infix fun Shape2d.xor(other: Shape2d): Shape2d = this.clipperOp(other, Clipper.ClipType.XOR)
infix fun Shape2d.difference(other: Shape2d): Shape2d = this.clipperOp(other, Clipper.ClipType.DIFFERENCE)

operator fun Shape2d.plus(other: Shape2d): Shape2d = this.clipperOp(other, Clipper.ClipType.UNION)
operator fun Shape2d.minus(other: Shape2d): Shape2d = this.clipperOp(other, Clipper.ClipType.DIFFERENCE)

fun Shape2d.extend(size: Double): Shape2d {
	val clipper = ClipperOffset()
	val solution = Paths()
	clipper.addPaths(this.paths, Clipper.JoinType.MITER, if (this.closed) Clipper.EndType.CLOSED_POLYGON else Clipper.EndType.OPEN_ROUND)
	clipper.execute(solution, size)
	return solution.toShape2d()
}

fun VectorPath.toPaths(): Paths {
	val paths = Paths()
	var path = Path()
	var lx = 0.0
	var ly = 0.0
	fun flushPath() {
		if (path.isNotEmpty()) {
			paths += path
			path = Path()
		}
	}
	this.visitCmds(
		moveTo = { x, y ->
			//kotlin.io.println("moveTo")
			path.addPoint(x, y)
			lx = x
			ly = y
		},
		lineTo = { x, y ->
			//kotlin.io.println("lineTo")
			path.addPoint(x, y)
			lx = x
			ly = y
		},
		quadTo = { x0, y0, x1, y1 ->
			//kotlin.io.println("quadTo")
			// @TODO: Optimize using control points
			val steps = 20
			val dt = 1.0 / steps
			for (n in 1 until steps) {
				path.add(Bezier.quadCalc(lx, ly, x0, y0, x1, y1, n * dt))
			}
			lx = x1
			ly = y1
		},
		cubicTo = { x0, y0, x1, y1, x2, y2 ->
			//kotlin.io.println("cubicTo")
			// @TODO: Optimize using control points
			val steps = 20
			val dt = 1.0 / steps
			for (n in 1 until steps) {
				path.add(Bezier.cubicCalc(lx, ly, x0, y0, x1, y1, x2, y2, n * dt))
			}
			lx = x2
			ly = y2
		},
		close = {
			if (path.isNotEmpty()) {
				path.add(path[0])
			}
			flushPath()
		}
	)
	flushPath()
	return paths
}

fun VectorPath.toShape2d() = this.toPaths().toShape2d()

fun Shape2d.getAllPoints() = this.paths.flatMap { it }
fun Shape2d.toPolygon() = if (this is Shape2d.Polygon) this else Shape2d.Polygon(this.getAllPoints())

fun List<Point2d>.triangulate(): List<Triangle> {
	val sc = SweepContext(this)
	val s = Sweep(sc)
	s.triangulate()
	return sc.triangles.toList()
}

fun Shape2d.triangulate(): List<Triangle> = this.getAllPoints().map { Point2d(it.x, it.y) }.triangulate()

fun List<Point2d>.containsPoint(x: Double, y: Double): Boolean {
	var intersections = 0
	for (n in 0 until this.size - 1) {
		val p1 = this[n + 0]
		val p2 = this[n + 1]
		intersections += HorizontalLine.intersectionsWithLine(x, y, p1.x, p1.y, p2.x, p2.y)
	}
	return (intersections % 2) != 0
}
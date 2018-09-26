package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.math.*
import kotlin.math.*

open class VectorPath(
	val commands: IntArrayList = IntArrayList(),
	val data: DoubleArrayList = DoubleArrayList(),
	val winding: Winding = Winding.EVEN_ODD
) {
	open fun clone(): VectorPath = VectorPath(IntArrayList(commands), DoubleArrayList(data), winding)

	interface Visitor {
		fun close()
		fun moveTo(x: Double, y: Double)
		fun lineTo(x: Double, y: Double)
		fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double)
		fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double)
	}

	inline fun visitCmds(
		moveTo: (x: Double, y: Double) -> Unit,
		lineTo: (x: Double, y: Double) -> Unit,
		quadTo: (x1: Double, y1: Double, x2: Double, y2: Double) -> Unit,
		cubicTo: (x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> Unit,
		close: () -> Unit
	) {
		var n = 0
		for (cmd in commands) {
			when (cmd) {
				Command.MOVE_TO -> {
					val x = data[n++]
					val y = data[n++]
					moveTo(x, y)
				}
				Command.LINE_TO -> {
					val x = data[n++]
					val y = data[n++]
					lineTo(x, y)
				}
				Command.QUAD_TO -> {
					val x1 = data[n++]
					val y1 = data[n++]
					val x2 = data[n++]
					val y2 = data[n++]
					quadTo(x1, y1, x2, y2)
				}
				Command.BEZIER_TO -> {
					val x1 = data[n++]
					val y1 = data[n++]
					val x2 = data[n++]
					val y2 = data[n++]
					val x3 = data[n++]
					val y3 = data[n++]
					cubicTo(x1, y1, x2, y2, x3, y3)
				}
				Command.CLOSE -> {
					close()
				}
			}
		}
	}

	inline fun visitEdges(
		line: (x0: Double, y0: Double, x1: Double, y1: Double) -> Unit,
		quad: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double) -> Unit,
		cubic: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> Unit,
		close: () -> Unit
	) {
		var mx = 0.0
		var my = 0.0
		var lx = 0.0
		var ly = 0.0
		visitCmds(
			moveTo = { x, y ->
				mx = x
				my = y
				lx = x
				ly = y
			},
			lineTo = { x, y ->
				line(lx, ly, x, y)
				lx = x
				ly = y
			},
			quadTo = { x1, y1, x2, y2 ->
				quad(lx, ly, x1, y1, x2, y2)
				lx = x2
				ly = y2
			},
			cubicTo = { x1, y1, x2, y2, x3, y3 ->
				cubic(lx, ly, x1, y1, x2, y2, x3, y3)
				lx = x3
				ly = y3
			},
			close = {
				if ((lx != mx) || (ly != my)) {
					line(lx, ly, mx, my)
				}
				close()
			}
		)
	}

	fun visit(visitor: Visitor) {
		visitCmds(
			moveTo = visitor::moveTo,
			lineTo = visitor::lineTo,
			quadTo = visitor::quadTo,
			cubicTo = visitor::cubicTo,
			close = visitor::close
		)
	}

	fun isEmpty(): Boolean = commands.isEmpty()
	fun isNotEmpty(): Boolean = commands.isNotEmpty()

	fun clear() {
		commands.clear()
		data.clear()
	}

	var lastX = 0.0; private set
	var lastY = 0.0; private set

	fun moveTo(p: Vector2) = moveTo(p.x, p.y)
	fun lineTo(p: Vector2) = lineTo(p.x, p.y)

	fun moveTo(x: Double, y: Double) {
		commands += Command.MOVE_TO
		data += x
		data += y
		lastX = x
		lastY = y
	}

	fun moveTo(x: Int, y: Int) = moveTo(x.toDouble(), y.toDouble())

	fun moveToH(x: Double) = moveTo(x, lastY)
	fun rMoveToH(x: Double) = moveTo(lastX + x, lastY)

	fun moveToV(y: Double) = moveTo(lastX, y)
	fun rMoveToV(y: Double) = moveTo(lastX, lastY + y)

	fun lineToH(x: Double) = lineTo(x, lastY)
	fun rLineToH(x: Double) = lineTo(lastX + x, lastY)

	fun lineToV(y: Double) = lineTo(lastX, y)
	fun rLineToV(y: Double) = lineTo(lastX, lastY + y)

	fun rMoveTo(x: Double, y: Double) = moveTo(this.lastX + x, this.lastY + y)
	fun rLineTo(x: Double, y: Double) = lineTo(this.lastX + x, this.lastY + y)

	fun rQuadTo(cx: Double, cy: Double, ax: Double, ay: Double) =
		quadTo(this.lastX + cx, this.lastY + cy, this.lastX + ax, this.lastY + ay)

	fun rCubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = cubicTo(
		this.lastX + cx1,
		this.lastY + cy1,
		this.lastX + cx2,
		this.lastY + cy2,
		this.lastX + ax,
		this.lastY + ay
	)

	private fun ensureMoveTo(x: Double, y: Double) {
		if (isEmpty()) {
			moveTo(x, y)
		}
	}

	fun lineTo(x: Double, y: Double) {
		ensureMoveTo(x, y)
		commands += Command.LINE_TO
		data += x
		data += y
		lastX = x
		lastY = y
	}

	fun lineTo(x: Int, y: Int) = lineTo(x.toDouble(), y.toDouble())

	fun quadTo(controlX: Int, controlY: Int, anchorX: Int, anchorY: Int) =
		quadTo(controlX.toDouble(), controlY.toDouble(), anchorX.toDouble(), anchorY.toDouble())

	fun cubicTo(cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int) =
		cubicTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())

	fun quadTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) {
		ensureMoveTo(controlX, controlY)
		commands += Command.QUAD_TO
		data += controlX
		data += controlY
		data += anchorX
		data += anchorY
		lastX = anchorX
		lastY = anchorY
	}

	fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
		ensureMoveTo(cx1, cy1)
		commands += Command.BEZIER_TO
		data += cx1
		data += cy1
		data += cx2
		data += cy2
		data += ax
		data += ay
		lastX = ax
		lastY = ay
	}

	//fun arcTo(b: Point2d, a: Point2d, c: Point2d, r: Double) {
	fun arcTo(ax: Double, ay: Double, cx: Double, cy: Double, r: Double) {
		ensureMoveTo(ax, ay)
		val bx = lastX
		val by = lastY
		val b = Vector2(bx, by)
		val a = Vector2(ax, ay)
		val c = Vector2(cx, cy)
		val PI_DIV_2 = PI / 2.0
		val AB = b - a
		val AC = c - a
		val angle = Vector2.angle(AB, AC) * 0.5
		val x = r * sin(PI_DIV_2 - angle) / sin(angle)
		val A = a + AB.unit * x
		val B = a + AC.unit * x
		lineTo(A.x, A.y)
		quadTo(a.x, a.y, B.x, B.y)
	}

	fun close() {
		commands += Command.CLOSE
	}

	fun rect(x: Double, y: Double, width: Double, height: Double) {
		moveTo(x, y)
		lineTo(x + width, y)
		lineTo(x + width, y + height)
		lineTo(x, y + height)
		close()
	}

	fun rectHole(x: Double, y: Double, width: Double, height: Double) {
		moveTo(x, y)
		lineTo(x, y + height)
		lineTo(x + width, y + height)
		lineTo(x + width, y)
		close()
	}

	fun roundRect(x: Double, y: Double, w: Double, h: Double, rx: Double, ry: Double = rx) {
		if (rx == 0.0 && ry == 0.0) {
			rect(x, y, w, h)
		} else {
			val r = if (w < 2 * rx) w / 2.0 else if (h < 2 * rx) h / 2.0 else rx
			this.moveTo(x + r, y)
			this.arcTo(x + w, y, x + w, y + h, r)
			this.arcTo(x + w, y + h, x, y + h, r)
			this.arcTo(x, y + h, x, y, r)
			this.arcTo(x, y, x + w, y, r)
		}
	}

	fun arc(x: Double, y: Double, r: Double, start: Double, end: Double) {
		// http://hansmuller-flex.blogspot.com.es/2011/04/approximating-circular-arc-with-cubic.html
		val EPSILON = 0.00001
		val PI_TWO = PI * 2
		val PI_OVER_TWO = PI / 2.0

		val startAngle = start % PI_TWO
		val endAngle = end % PI_TWO
		var remainingAngle = Math.min(PI_TWO, abs(endAngle - startAngle))
		if (remainingAngle == 0.0 && start != end) remainingAngle = PI_TWO
		val sgn = if (startAngle < endAngle) 1 else -1
		var a1 = startAngle
		val p1 = MVector2()
		val p2 = MVector2()
		val p3 = MVector2()
		val p4 = MVector2()
		var index = 0
		while (remainingAngle > EPSILON) {
			val a2 = a1 + sgn * Math.min(remainingAngle, PI_OVER_TWO)

			val k = 0.5522847498
			val a = (a2 - a1) / 2.0
			val x4 = r * cos(a)
			val y4 = r * sin(a)
			val x1 = x4
			val y1 = -y4
			val f = k * tan(a)
			val x2 = x1 + f * y4
			val y2 = y1 + f * x4
			val x3 = x2
			val y3 = -y2
			val ar = a + a1
			val cos_ar = cos(ar)
			val sin_ar = sin(ar)
			p1.setTo(x + r * cos(a1), y + r * sin(a1))
			p2.setTo(x + x2 * cos_ar - y2 * sin_ar, y + x2 * sin_ar + y2 * cos_ar)
			p3.setTo(x + x3 * cos_ar - y3 * sin_ar, y + x3 * sin_ar + y3 * cos_ar)
			p4.setTo(x + r * cos(a2), y + r * sin(a2))

			if (index == 0) moveTo(p1.x, p1.y)
			cubicTo(p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)

			index++
			remainingAngle -= abs(a2 - a1)
			a1 = a2
		}
		if (startAngle == endAngle && index != 0) {
			close()
		}
	}

	fun circle(x: Double, y: Double, radius: Double) = arc(x, y, radius, 0.0, PI * 2.0)

	fun ellipse(x: Double, y: Double, rw: Double, rh: Double) {
		val k = .5522848
		val ox = (rw / 2) * k
		val oy = (rh / 2) * k
		val xe = x + rw
		val ye = y + rh
		val xm = x + rw / 2
		val ym = y + rh / 2
		moveTo(x, ym)
		cubicTo(x, ym - oy, xm - ox, y, xm, y)
		cubicTo(xm + ox, y, xe, ym - oy, xe, ym)
		cubicTo(xe, ym + oy, xm + ox, ye, xm, ye)
		cubicTo(xm - ox, ye, x, ym + oy, x, ym)
	}

	fun addBounds(bb: BoundsBuilder): Unit {
		var lx = 0.0
		var ly = 0.0

		visitCmds(
			moveTo = { x, y ->
				bb.add(x, y)
				lx = x
				ly = y
			},
			lineTo = { x, y ->
				bb.add(x, y)
				lx = x
				ly = y
			},
			quadTo = { cx, cy, ax, ay ->
				bb.add(Bezier.quadBounds(lx, ly, cx, cy, ax, ay, bb.tempRect))
				lx = ax
				ly = ay
			},
			cubicTo = { cx1, cy1, cx2, cy2, ax, ay ->
				bb.add(Bezier.cubicBounds(lx, ly, cx1, cy1, cx2, cy2, ax, ay, bb.tempRect))
				lx = ax
				ly = ay
			},
			close = {

			}
		)
	}

	fun getBounds(out: Rectangle = Rectangle(), bb: BoundsBuilder = BoundsBuilder()): Rectangle {
		bb.reset()
		addBounds(bb)
		return bb.getBounds(out)
	}

	fun getPoints(): List<Point2d> {
		val points = arrayListOf<Point2d>()
		this.visitCmds(
			moveTo = { x, y -> points += Point2d(x, y) },
			lineTo = { x, y -> points += Point2d(x, y) },
			quadTo = { x1, y1, x2, y2 -> points += Point2d(x2, y2) },
			cubicTo = { x1, y1, x2, y2, x3, y3 -> points += Point2d(x3, y3) },
			close = { }
		)
		return points
	}

	private val p1 = MVector2()
	private val p2 = MVector2()

	// http://erich.realtimerendering.com/ptinpoly/
	// http://stackoverflow.com/questions/217578/how-can-i-determine-whether-a-2d-point-is-within-a-polygon/2922778#2922778
	// https://www.particleincell.com/2013/cubic-line-intersection/
	// I run a semi-infinite ray horizontally (increasing x, fixed y) out from the test point, and count how many edges it crosses.
	// At each crossing, the ray switches between inside and outside. This is called the Jordan curve theorem.
	fun containsPoint(x: Double, y: Double): Boolean {
		val testx = x
		val testy = y

		var intersections = 0

		visitEdges(
			line = { x0, y0, x1, y1 -> intersections += HorizontalLine.intersectionsWithLine(
				testx, testy, x0, y0, x1, y1
			) },
			quad = { x0, y0, x1, y1, x2, y2 -> intersections += HorizontalLine.interesectionsWithQuadBezier(
				testx, testy, x0, y0, x1, y1, x2, y2, p1, p2
			) },
			cubic = { x0, y0, x1, y1, x2, y2, x3, y3 -> intersections += HorizontalLine.intersectionsWithCubicBezier(
				testx, testy, x0, y0, x1, y1, x2, y2, x3, y3, p1, p2
			) },
			close = {}
		)
		return (intersections % 2) != 0
	}

	fun containsPoint(x: Int, y: Int): Boolean = containsPoint(x.toDouble(), y.toDouble())


	object Command {
		//val CUBIC_CURVE_TO = 6
		val MOVE_TO = 1
		val LINE_TO = 2
		val QUAD_TO = 3
		val BEZIER_TO = 4
		val CLOSE = 5
		//val NO_OP = 0
		//val WIDE_LINE_TO = 5
		//val WIDE_MOVE_TO = 4
	}

	enum class Winding(val str: String) {
		EVEN_ODD("evenOdd"), NON_ZERO("nonZero");
	}

	fun write(path: VectorPath) {
		this.commands += path.commands
		this.data += path.data
		this.lastX = path.lastX
		this.lastY = path.lastY
	}
}


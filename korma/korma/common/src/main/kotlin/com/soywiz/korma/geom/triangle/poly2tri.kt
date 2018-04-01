package com.soywiz.korma.geom.triangle

import com.soywiz.korma.Vector2
import com.soywiz.korma.geom.Orientation
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.math.Math
import kotlin.math.PI
import kotlin.math.atan2

class AdvancingFront(
	var head: Node,
	var tail: Node
) {
	var search_node: Node = head

	/*fun findSearchNode(x) {
		return this.search_node;
	}*/

	fun locateNode(x: Double): Node? {
		var node: Node = this.search_node

		if (x < node.value) {
			while (node.prev != null) {
				node = node.prev!!
				if (x >= node.value) {
					this.search_node = node
					return node
				}
			}
		} else {
			while (node.next != null) {
				node = node.next!!
				if (x < node.value) {
					this.search_node = node.prev!!
					return node.prev!!
				}
			}
		}
		return null
	}

	fun locatePoint(point: Point2d): Node? {
		val px: Double = point.x
		//var node:* = this.FindSearchNode(px);
		var node: Node? = this.search_node
		val nx: Double = node!!.point.x

		if (px == nx) {
			if (point != (node.point)) {
				// We might have two nodes with same x value for a short time
				if (point == (node.prev!!.point)) {
					node = node.prev
				} else if (point == (node.next!!.point)) {
					node = node.next
				} else {
					throw(Error("Invalid AdvancingFront.locatePoint call!"))
				}
			}
		} else if (px < nx) {
			node = node.prev
			while (node != null) {
				if (point == (node.point)) break
				node = node.prev
			}
		} else {
			node = node.next
			while (node != null) {
				if (point == (node.point)) break
				node = node.next
			}
		}

		if (node != null) this.search_node = node
		return node
	}

}

class Basin {
	var left_node: Node? = null
	var bottom_node: Node? = null
	var right_node: Node? = null
	var width: Double = 0.0
	var left_highest: Boolean = false

	fun clear(): Unit {
		this.left_node = null
		this.bottom_node = null
		this.right_node = null
		this.width = 0.0
		this.left_highest = false
	}
}

object Constants {
	/*
	 * Inital triangle factor, seed triangle will extend 30% of
	 * PointSet width to both left and right.
	 */
	const val kAlpha: Double = 0.3
	const val EPSILON: Double = 1e-12
	const val PI_2: Double = PI / 2
	const val PI_3div4: Double = 3 * PI / 4
}

class Edge(
	var p1: Point2d,
	var p2: Point2d,
	val ctx: EdgeContext
) {
	var p: Point2d
	var q: Point2d

	/// Constructor
	init {
		var swap: Boolean = false

		if (p1.y > p2.y) {
			swap = true
		} else if (p1.y == p2.y) {
			if (p1.x == p2.x) throw Error("Repeat points")
			swap = (p1.x > p2.x)
		} else {
			swap = false
		}

		if (swap) {
			this.q = p1
			this.p = p2
		} else {
			this.p = p1
			this.q = p2
		}

		ctx.getPointEdgeList(this.q).add(this)
	}

	fun hasPoint(point: Point2d): Boolean = (p == point) || (q == point)

	companion object {
		fun getUniquePointsFromEdges(edges: ArrayList<Edge>): List<Point2d> = edges.flatMap { listOf(it.p, it.q) }.distinct()

		fun traceList(edges: ArrayList<Edge>): Unit {
			val pointsList = Companion.getUniquePointsFromEdges(edges)
			val pointsMap = hashMapOf<Point2d, Int>()

			var points_length = 0
			for (point in pointsList) pointsMap[point] = ++points_length

			fun getPointName(point: Point2d): String = "p" + pointsMap[point]

			println("Points:")
			for (point in pointsList) println("  " + getPointName(point) + " = " + point)
			println("Edges:")
			for (edge in edges) println("  Edge(" + getPointName(edge.p) + ", " + getPointName(edge.q) + ")")
		}
	}


	override fun toString(): String = "Edge(${this.p}, ${this.q})"
}

class EdgeEvent {
	var constrained_edge: Edge? = null
	var right: Boolean = false
}

class Node(
	var point: Point2d,
	var triangle: Triangle? = null
) {
	var prev: Node? = null
	var next: Node? = null
	var value: Double = this.point.x

	/**
	 *
	 * @param node - middle node
	 * @return the angle between 3 front nodes
	 */
	val holeAngle: Double
		get() {
			/* Complex plane
			 * ab = cosA +i*sinA
			 * ab = (ax + ay*i)(bx + by*i) = (ax*bx + ay*by) + i(ax*by-ay*bx)
			 * atan2(y,x) computes the principal value of the argument function
			 * applied to the complex number x+iy
			 * Where x = ax*bx + ay*by
			 *       y = ax*by - ay*bx
			 */
			val prev = this.prev ?: throw IllegalStateException("Not enough vertices")
			val next = this.next ?: throw IllegalStateException("Not enough vertices")
			val ax: Double = next.point.x - this.point.x
			val ay: Double = next.point.y - this.point.y
			val bx: Double = prev.point.x - this.point.x
			val by: Double = prev.point.y - this.point.y
			return atan2(ax * by - ay * bx,
				ax * bx + ay * by
			)
		}

	val basinAngle: Double
		get() {
			val nextNext = this.next?.next ?: throw IllegalStateException("Not enough vertices")
			return atan2(this.point.y - nextNext.point.y, // ay
				this.point.x - nextNext.point.x  // ax
			)
		}
}

class EdgeContext {
	val pointsToEdgeLists = hashMapOf<Point2d, ArrayList<Edge>>()
	fun getPointEdgeList(point: Point2d) = pointsToEdgeLists.getOrPut(point) { arrayListOf() }
	fun createEdge(p1: Point2d, p2: Point2d): Edge = Edge(p1, p2, this)
}

class Sweep(
	protected var context: SweepContext
) {
	val edgeContext get() = context.edgeContext
	/**
	 * Triangulate simple polygon with holes.
	 * @param   tcx SweepContext object.
	 */
	fun triangulate(): Unit {
		context.initTriangulation()
		context.createAdvancingFront()
		sweepPoints()                    // Sweep points; build mesh
		finalizationPolygon()            // Clean up
	}

	fun sweepPoints(): Unit {
		for (i in 1 until this.context.points.size) {
			val point: Point2d = this.context.points[i]
			val node: Node = this.pointEvent(point)
			val edgeList = edgeContext.getPointEdgeList(point)
			for (j in 0 until edgeList.size) {
				this.edgeEventByEdge(edgeList[j], node)
			}
		}
	}

	fun finalizationPolygon(): Unit {
		// Get an Internal triangle to start with
		val next = this.context.front.head.next!!
		var t: Triangle = next.triangle!!
		val p: Point2d = next.point
		while (!t.getConstrainedEdgeCW(p)) t = t.neighborCCW(p)!!

		// Collect interior triangles constrained by edges
		this.context.meshClean(t)
	}

	/**
	 * Find closes node to the left of the point and
	 * create a triangle. If needed holes and basins
	 * will be filled to.
	 */
	fun pointEvent(point: Point2d): Node {
		val node = this.context.locateNode(point)!!
		val new_node = newFrontTriangle(point, node)

		// Only need to check +epsilon since point never have smaller
		// x value than node due to how we fetch nodes from the front
		if (point.x <= (node.point.x + Constants.EPSILON)) fill(node)

		//tcx.AddNode(new_node);

		fillAdvancingFront(new_node)
		return new_node
	}

	fun edgeEventByEdge(edge: Edge, node: Node): Unit {
		val edge_event = this.context.edge_event
		edge_event.constrained_edge = edge
		edge_event.right = (edge.p.x > edge.q.x)

		val triangle = node.triangle!!

		if (triangle.isEdgeSide(edge.p, edge.q)) return

		// For now we will do all needed filling
		// TODO: integrate with flip process might give some better performance
		//       but for now this avoid the issue with cases that needs both flips and fills
		this.fillEdgeEvent(edge, node)

		this.edgeEventByPoints(edge.p, edge.q, triangle, edge.q)
	}

	fun edgeEventByPoints(ep: Point2d, eq: Point2d, triangle: Triangle, point: Point2d): Unit {
		if (triangle.isEdgeSide(ep, eq)) return

		val p1: Point2d = triangle.pointCCW(point)
		val o1: Orientation = Orientation.orient2d(eq, p1, ep)
		if (o1 == Orientation.COLLINEAR) throw(Error("Sweep.edgeEvent: Collinear not supported!"))

		val p2: Point2d = triangle.pointCW(point)
		val o2: Orientation = Orientation.orient2d(eq, p2, ep)
		if (o2 == Orientation.COLLINEAR) throw(Error("Sweep.edgeEvent: Collinear not supported!"))

		if (o1 == o2) {
			// Need to decide if we are rotating CW or CCW to get to a triangle
			// that will cross edge
			edgeEventByPoints(ep, eq, if (o1 == Orientation.CW) triangle.neighborCCW(point)!! else triangle.neighborCW(point)!!, point)
		} else {
			// This triangle crosses constraint so lets flippin start!
			flipEdgeEvent(ep, eq, triangle, point)
		}
	}

	fun newFrontTriangle(point: Point2d, node: Node): Node {
		val triangle: Triangle = Triangle(point, node.point, node.next!!.point, edgeContext)

		triangle.markNeighborTriangle(node.triangle!!)
		this.context.addToSet(triangle)

		val new_node: Node = Node(point)
		new_node.next = node.next
		new_node.prev = node
		node.next!!.prev = new_node
		node.next = new_node

		if (!legalize(triangle)) this.context.mapTriangleToNodes(triangle)

		return new_node
	}

	/**
	 * Adds a triangle to the advancing front to fill a hole.
	 * @param tcx
	 * @param node - middle node, that is the bottom of the hole
	 */
	fun fill(node: Node): Unit {
		val triangle: Triangle = Triangle(node.prev!!.point, node.point, node.next!!.point, edgeContext)

		// TODO: should copy the constrained_edge value from neighbor triangles
		//       for now constrained_edge values are copied during the legalize
		triangle.markNeighborTriangle(node.prev!!.triangle!!)
		triangle.markNeighborTriangle(node.triangle!!)

		this.context.addToSet(triangle)

		// Update the advancing front
		node.prev!!.next = node.next
		node.next!!.prev = node.prev

		// If it was legalized the triangle has already been mapped
		if (!legalize(triangle)) {
			this.context.mapTriangleToNodes(triangle)
		}

		this.context.removeNode(node)
	}

	/**
	 * Fills holes in the Advancing Front
	 */
	fun fillAdvancingFront(n: Node): Unit {
		var node: Node
		var angle: Double

		// Fill right holes
		node = n.next!!
		while (node.next != null) {
			angle = node.holeAngle
			if ((angle > Constants.PI_2) || (angle < -Constants.PI_2)) break
			this.fill(node)
			node = node.next!!
		}

		// Fill left holes
		node = n.prev!!
		while (node.prev != null) {
			angle = node.holeAngle
			if ((angle > Constants.PI_2) || (angle < -Constants.PI_2)) break
			this.fill(node)
			node = node.prev!!
		}

		// Fill right basins
		if ((n.next != null) && (n.next!!.next != null)) {
			angle = n.basinAngle
			if (angle < Constants.PI_3div4) this.fillBasin(n)
		}
	}

	/**
	 * Returns true if triangle was legalized
	 */
	fun legalize(t: Triangle): Boolean {
		// To legalize a triangle we start by finding if any of the three edges
		// violate the Delaunay condition
		for (i in 0 until 3) {
			if (t.delaunay_edge[i]) continue
			val ot: Triangle = t.neighbors[i] ?: continue
			val p: Point2d = t.points[i]
			val op: Point2d = ot.oppositePoint(t, p)
			val oi: Int = ot.index(op)

			// If this is a Constrained Edge or a Delaunay Edge(only during recursive legalization)
			// then we should not try to legalize
			if (ot.constrained_edge[oi] || ot.delaunay_edge[oi]) {
				t.constrained_edge[i] = ot.constrained_edge[oi]
				continue
			}

			if (Triangle.insideIncircle(p, t.pointCCW(p), t.pointCW(p), op)) {
				// Lets mark this shared edge as Delaunay
				t.delaunay_edge[i] = true
				ot.delaunay_edge[oi] = true

				// Lets rotate shared edge one vertex CW to legalize it
				Triangle.rotateTrianglePair(t, p, ot, op)

				// We now got one valid Delaunay Edge shared by two triangles
				// This gives us 4 edges to check for Delaunay

				var not_legalized: Boolean

				// Make sure that triangle to node mapping is done only one time for a specific triangle
				not_legalized = !this.legalize(t)
				if (not_legalized) this.context.mapTriangleToNodes(t)

				not_legalized = !this.legalize(ot)
				if (not_legalized) this.context.mapTriangleToNodes(ot)

				// Reset the Delaunay edges, since they only are valid Delaunay edges
				// until we add a triangle or point.
				// XXX: need to think about this. Can these edges be tried after we
				//      return to previous recursive level?
				t.delaunay_edge[i] = false
				ot.delaunay_edge[oi] = false

				// If triangle have been legalized no need to check the other edges since
				// the recursive legalization will handles those so we can end here.
				return true
			}
		}
		return false
	}

	/**
	 * Fills a basin that has formed on the Advancing Front to the right
	 * of given node.<br>
	 * First we decide a left,bottom and right node that forms the
	 * boundaries of the basin. Then we do a reqursive fill.
	 *
	 * @param tcx
	 * @param node - starting node, this or next node will be left node
	 */
	fun fillBasin(node: Node): Unit {
		val context = this.context
		val basin = context.basin
		basin.left_node = if (Orientation.orient2d(node.point, node.next!!.point, node.next!!.next!!.point) == Orientation.CCW) node.next!!.next else node.next

		// Find the bottom and right node
		basin.bottom_node = basin.left_node
		while ((basin.bottom_node!!.next != null) && (basin.bottom_node!!.point.y >= basin.bottom_node!!.next!!.point.y)) {
			basin.bottom_node = basin.bottom_node!!.next
		}

		// No valid basin
		if (basin.bottom_node == basin.left_node) return

		basin.right_node = basin.bottom_node
		while ((basin.right_node!!.next != null) && (basin.right_node!!.point.y < basin.right_node!!.next!!.point.y)) {
			basin.right_node = basin.right_node!!.next
		}

		// No valid basins
		if (basin.right_node == basin.bottom_node) return

		basin.width = (basin.right_node!!.point.x - basin.left_node!!.point.x)
		basin.left_highest = (basin.left_node!!.point.y > basin.right_node!!.point.y)

		this.fillBasinReq(basin.bottom_node!!)
	}

	/**
	 * Recursive algorithm to fill a Basin with triangles
	 *
	 * @param tcx
	 * @param node - bottom_node
	 */
	fun fillBasinReq(node: Node): Unit {
		@Suppress("NAME_SHADOWING")
		var node = node
		// if shallow stop filling
		if (this.isShallow(node)) return

		this.fill(node)

		if (node.prev == this.context.basin.left_node && node.next == this.context.basin.right_node) {
			return
		} else if (node.prev == this.context.basin.left_node) {
			if (Orientation.orient2d(node.point, node.next!!.point, node.next!!.next!!.point) == Orientation.CW) return
			node = node.next!!
		} else if (node.next == this.context.basin.right_node) {
			if (Orientation.orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point) == Orientation.CCW) return
			node = node.prev!!
		} else {
			// Continue with the neighbor node with lowest Y value
			node = if (node.prev!!.point.y < node.next!!.point.y) node.prev!! else node.next!!
		}

		this.fillBasinReq(node)
	}

	fun isShallow(node: Node): Boolean {
		val height: Double = if (this.context.basin.left_highest) {
			this.context.basin.left_node!!.point.y - node.point.y
		} else {
			this.context.basin.right_node!!.point.y - node.point.y
		}

		// if shallow stop filling
		return (this.context.basin.width > height)
	}

	fun fillEdgeEvent(edge: Edge, node: Node): Unit {
		if (this.context.edge_event.right) {
			this.fillRightAboveEdgeEvent(edge, node)
		} else {
			this.fillLeftAboveEdgeEvent(edge, node)
		}
	}

	fun fillRightAboveEdgeEvent(edge: Edge, node: Node): Unit {
		var node = node
		while (node.next!!.point.x < edge.p.x) {
			// Check if next node is below the edge
			if (Orientation.orient2d(edge.q, node.next!!.point, edge.p) == Orientation.CCW) {
				this.fillRightBelowEdgeEvent(edge, node)
			} else {
				node = node.next!!
			}
		}
	}

	fun fillRightBelowEdgeEvent(edge: Edge, node: Node): Unit {
		if (node.point.x >= edge.p.x) return
		if (Orientation.orient2d(node.point, node.next!!.point, node.next!!.next!!.point) == Orientation.CCW) {
			// Concave
			this.fillRightConcaveEdgeEvent(edge, node)
		} else {
			this.fillRightConvexEdgeEvent(edge, node) // Convex
			this.fillRightBelowEdgeEvent(edge, node) // Retry this one
		}
	}

	fun fillRightConcaveEdgeEvent(edge: Edge, node: Node): Unit {
		this.fill(node.next!!)
		if (node.next!!.point != edge.p) {
			// Next above or below edge?
			if (Orientation.orient2d(edge.q, node.next!!.point, edge.p) == Orientation.CCW) {
				// Below
				if (Orientation.orient2d(node.point, node.next!!.point, node.next!!.next!!.point) == Orientation.CCW) {
					// Next is concave
					this.fillRightConcaveEdgeEvent(edge, node)
				} else {
					// Next is convex
				}
			}
		}
	}

	fun fillRightConvexEdgeEvent(edge: Edge, node: Node): Unit {
		// Next concave or convex?
		if (Orientation.orient2d(node.next!!.point, node.next!!.next!!.point, node.next!!.next!!.next!!.point) == Orientation.CCW) {
			// Concave
			this.fillRightConcaveEdgeEvent(edge, node.next!!)
		} else {
			// Convex
			// Next above or below edge?
			if (Orientation.orient2d(edge.q, node.next!!.next!!.point, edge.p) == Orientation.CCW) {
				// Below
				this.fillRightConvexEdgeEvent(edge, node.next!!)
			} else {
				// Above
			}
		}
	}

	fun fillLeftAboveEdgeEvent(edge: Edge, node: Node) {
		var node = node
		while (node.prev!!.point.x > edge.p.x) {
			// Check if next node is below the edge
			if (Orientation.orient2d(edge.q, node.prev!!.point, edge.p) == Orientation.CW) {
				this.fillLeftBelowEdgeEvent(edge, node)
			} else {
				node = node.prev!!
			}
		}
	}

	fun fillLeftBelowEdgeEvent(edge: Edge, node: Node): Unit {
		if (node.point.x > edge.p.x) {
			if (Orientation.orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point) == Orientation.CW) {
				// Concave
				this.fillLeftConcaveEdgeEvent(edge, node)
			} else {
				// Convex
				this.fillLeftConvexEdgeEvent(edge, node)
				// Retry this one
				this.fillLeftBelowEdgeEvent(edge, node)
			}
		}
	}

	fun fillLeftConvexEdgeEvent(edge: Edge, node: Node): Unit {
		// Next concave or convex?
		if (Orientation.orient2d(node.prev!!.point, node.prev!!.prev!!.point, node.prev!!.prev!!.prev!!.point) == Orientation.CW) {
			// Concave
			this.fillLeftConcaveEdgeEvent(edge, node.prev!!)
		} else {
			// Convex
			// Next above or below edge?
			if (Orientation.orient2d(edge.q, node.prev!!.prev!!.point, edge.p) == Orientation.CW) {
				// Below
				this.fillLeftConvexEdgeEvent(edge, node.prev!!)
			} else {
				// Above
			}
		}
	}

	fun fillLeftConcaveEdgeEvent(edge: Edge, node: Node): Unit {
		this.fill(node.prev!!)
		if (node.prev!!.point != edge.p) {
			// Next above or below edge?
			if (Orientation.orient2d(edge.q, node.prev!!.point, edge.p) == Orientation.CW) {
				// Below
				if (Orientation.orient2d(node.point, node.prev!!.point, node.prev!!.prev!!.point) == Orientation.CW) {
					// Next is concave
					this.fillLeftConcaveEdgeEvent(edge, node)
				} else {
					// Next is convex
				}
			}
		}
	}

	fun flipEdgeEvent(ep: Point2d, eq: Point2d, t: Triangle, p: Point2d): Unit {
		var t = t
		val ot: Triangle = t.neighborAcross(p) ?: throw Error("[BUG:FIXME] FLIP failed due to missing triangle!")
		// If we want to integrate the fillEdgeEvent do it here
		// With current implementation we should never get here

		val op: Point2d = ot.oppositePoint(t, p)

		if (Triangle.inScanArea(p, t.pointCCW(p), t.pointCW(p), op)) {
			// Lets rotate shared edge one vertex CW
			Triangle.rotateTrianglePair(t, p, ot, op)
			this.context.mapTriangleToNodes(t)
			this.context.mapTriangleToNodes(ot)

			// @TODO: equals?
			if ((p == eq) && (op == ep)) {
				if ((eq == this.context.edge_event.constrained_edge!!.q) && (ep == this.context.edge_event.constrained_edge!!.p)) {
					t.markConstrainedEdgeByPoints(ep, eq)
					ot.markConstrainedEdgeByPoints(ep, eq)
					this.legalize(t)
					this.legalize(ot)
				} else {
					// XXX: I think one of the triangles should be legalized here?
				}
			} else {
				val o: Orientation = Orientation.orient2d(eq, op, ep)
				t = this.nextFlipTriangle(o, t, ot, p, op)
				this.flipEdgeEvent(ep, eq, t, p)
			}
		} else {
			val newP: Point2d = Companion.nextFlipPoint(ep, eq, ot, op)
			this.flipScanEdgeEvent(ep, eq, t, ot, newP)
			this.edgeEventByPoints(ep, eq, t, p)
		}
	}

	fun nextFlipTriangle(o: Orientation, t: Triangle, ot: Triangle, p: Point2d, op: Point2d): Triangle {
		if (o == Orientation.CCW) {
			// ot is not crossing edge after flip
			ot.delaunay_edge[ot.edgeIndex(p, op)] = true
			this.legalize(ot)
			ot.clearDelunayEdges()
			return t
		} else {
			// t is not crossing edge after flip
			t.delaunay_edge[t.edgeIndex(p, op)] = true
			this.legalize(t)
			t.clearDelunayEdges()
			return ot
		}
	}

	companion object {
		fun nextFlipPoint(ep: Point2d, eq: Point2d, ot: Triangle, op: Point2d): Point2d {
			return when (Orientation.orient2d(eq, op, ep)) {
				Orientation.CW -> ot.pointCCW(op) // Right
				Orientation.CCW -> ot.pointCW(op) // Left
				else -> throw Error("[Unsupported] Sweep.NextFlipPoint: opposing point on constrained edge!")
			}
		}
	}

	fun flipScanEdgeEvent(ep: Point2d, eq: Point2d, flip_triangle: Triangle, t: Triangle, p: Point2d): Unit {
		val ot = t.neighborAcross(p) ?: throw Error("[BUG:FIXME] FLIP failed due to missing triangle") // If we want to integrate the fillEdgeEvent do it here With current implementation we should never get here

		val op = ot.oppositePoint(t, p)

		if (Triangle.inScanArea(eq, flip_triangle.pointCCW(eq), flip_triangle.pointCW(eq), op)) {
			// flip with edge op.eq
			this.flipEdgeEvent(eq, op, ot, op)
			// TODO: Actually I just figured out that it should be possible to
			//       improve this by getting the next ot and op before the the above
			//       flip and continue the flipScanEdgeEvent here
			// set ot and op here and loop back to inScanArea test
			// also need to set a flip_triangle first
			// Turns out at first glance that this is somewhat complicated
			// so it will have to wait.
		} else {
			val newP: Point2d = nextFlipPoint(ep, eq, ot, op)
			this.flipScanEdgeEvent(ep, eq, flip_triangle, ot, newP)
		}
	}
}

class SweepContext() {
	var triangles: ArrayList<Triangle> = ArrayList<Triangle>()
	var points: ArrayList<Point2d> = ArrayList<Point2d>()
	var edge_list: ArrayList<Edge> = ArrayList<Edge>()
	val edgeContext = EdgeContext()

	val set = LinkedHashSet<Triangle>()

	lateinit var front: AdvancingFront
	lateinit var head: Point2d
	lateinit var tail: Point2d

	lateinit var af_head: Node
	lateinit var af_middle: Node
	lateinit var af_tail: Node

	val basin: Basin = Basin()
	var edge_event: EdgeEvent = EdgeEvent()

	constructor(polyline: List<Point2d>) : this() {
		this.addPolyline(polyline)
	}

	protected fun addPoints(points: List<Point2d>) {
		for (point in points) this.points.add(point)
	}

	fun addPolyline(polyline: List<Point2d>): Unit {
		this.initEdges(polyline)
		this.addPoints(polyline)
	}

	/**
	 * An alias of addPolyline.
	 *
	 * @param    polyline
	 */
	fun addHole(polyline: List<Point2d>): Unit {
		addPolyline(polyline)
	}

	protected fun initEdges(polyline: List<Point2d>): Unit {
		for (n in 0 until polyline.size) {
			this.edge_list.add(Edge(polyline[n], polyline[(n + 1) % polyline.size], edgeContext))
		}
	}

	fun addToSet(triangle: Triangle): Unit {
		this.set += triangle
	}

	fun initTriangulation(): Unit {
		var xmin: Double = this.points[0].x
		var xmax: Double = this.points[0].x
		var ymin: Double = this.points[0].y
		var ymax: Double = this.points[0].y

		// Calculate bounds
		for (p in this.points) {
			if (p.x > xmax) xmax = p.x
			if (p.x < xmin) xmin = p.x
			if (p.y > ymax) ymax = p.y
			if (p.y < ymin) ymin = p.y
		}

		val dx: Double = Constants.kAlpha * (xmax - xmin)
		val dy: Double = Constants.kAlpha * (ymax - ymin)
		this.head = Point2d(xmax + dx, ymin - dy)
		this.tail = Point2d(xmin - dy, ymin - dy)

		// Sort points along y-axis
		Vector2.sortPoints(this.points)
		//throw(Error("@TODO Implement 'Sort points along y-axis' @see class SweepContext"));
	}

	fun locateNode(point: Point2d): Node? = this.front.locateNode(point.x)

	fun createAdvancingFront(): Unit {
		// Initial triangle
		val triangle: Triangle = Triangle(this.points[0], this.tail, this.head, edgeContext)

		addToSet(triangle)

		val head: Node = Node(triangle.points[1], triangle)
		val middle: Node = Node(triangle.points[0], triangle)
		val tail: Node = Node(triangle.points[2])

		this.front = AdvancingFront(head, tail)

		head.next = middle
		middle.next = tail
		middle.prev = head
		tail.prev = middle
	}

	fun removeNode(node: Node): Unit {
		// do nothing
	}

	fun mapTriangleToNodes(triangle: Triangle): Unit {
		for (n in 0 until 3) {
			if (triangle.neighbors[n] == null) {
				val neighbor: Node? = this.front.locatePoint(triangle.pointCW(triangle.points[n]))
				if (neighbor != null) neighbor.triangle = triangle
			}
		}
	}

	fun removeFromMap(triangle: Triangle): Unit {
		this.set -= triangle
	}

	fun meshClean(triangle: Triangle?, level: Int = 0): Unit {
		if (level == 0) {
			//for each (var mappedTriangle:Triangle in this.map) println(mappedTriangle);
		}
		if (triangle == null || triangle.interior) return
		triangle.interior = true
		this.triangles.add(triangle)
		for (n in 0 until 3) {
			if (!triangle.constrained_edge[n]) {
				this.meshClean(triangle.neighbors[n], level + 1)
			}
		}
	}
}

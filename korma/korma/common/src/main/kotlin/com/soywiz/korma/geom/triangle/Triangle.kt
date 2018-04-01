package com.soywiz.korma.geom.triangle

import com.soywiz.korma.geom.Orientation
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.math.Math
import kotlin.math.abs

data class Triangle(
	var p1: Point2d,
	var p2: Point2d,
	var p3: Point2d,
	val ctx: EdgeContext = EdgeContext(),
	var fixOrientation: Boolean = false,
	var checkOrientation: Boolean = true
) {
	var points = arrayOf(p1, p2, p3)
	var neighbors = arrayOfNulls<Triangle>(3) // Neighbor list
	var interior: Boolean = false // Has this triangle been marked as an interior triangle?
	var constrained_edge = Array<Boolean>(3) { false } // Flags to determine if an edge is a Constrained edge
	var delaunay_edge = Array<Boolean>(3) { false } // Flags to determine if an edge is a Delauney edge

	init {
		@Suppress("NAME_SHADOWING")
		var p2 = p2
		@Suppress("NAME_SHADOWING")
		var p3 = p3
		if (fixOrientation) {
			if (Orientation.orient2d(p1, p2, p3) == Orientation.CW) {
				val pt = p3
				p3 = p2
				p2 = pt
				//println("Fixed orientation");
			}
		}
		if (checkOrientation && Orientation.orient2d(p3, p2, p1) != Orientation.CW) throw(Error("Triangle must defined with Orientation.CW"))
		this.points[0] = p1
		this.points[1] = p2
		this.points[2] = p3
	}

	/**
	 * Test if this Triangle contains the Point2d object given as parameter as its vertices.
	 *
	 * @return <code>True</code> if the Point2d objects are of the Triangle's vertices,
	 *         <code>false</code> otherwise.
	 */
	fun containsPoint(point: Point2d): Boolean = point == (points[0]) || point == (points[1]) || point == (points[2])

	/**
	 * Test if this Triangle contains the Edge object given as parameters as its bounding edges.
	 * @return <code>True</code> if the Edge objects are of the Triangle's bounding
	 *         edges, <code>false</code> otherwise.
	 */
	// In a triangle to check if contains and edge is enough to check if it contains the two vertices.
	fun containsEdge(edge: Edge): Boolean = containsEdgePoints(edge.p, edge.q)

	// In a triangle to check if contains and edge is enough to check if it contains the two vertices.
	fun containsEdgePoints(p1: Point2d, p2: Point2d): Boolean = containsPoint(p1) && containsPoint(p2)

	/**
	 * Update neighbor pointers.<br>
	 * This method takes either 3 parameters (<code>p1</code>, <code>p2</code> and
	 * <code>t</code>) or 1 parameter (<code>t</code>).
	 * @param   t   Triangle object.
	 * @param   p1  Point2d object.
	 * @param   p2  Point2d object.
	 */
	fun markNeighbor(t: Triangle, p1: Point2d, p2: Point2d): Unit {
		if ((p1 == (this.points[2]) && p2 == (this.points[1])) || (p1 == (this.points[1]) && p2 == (this.points[2]))) {
			this.neighbors[0] = t
			return
		}
		if ((p1 == (this.points[0]) && p2 == (this.points[2])) || (p1 == (this.points[2]) && p2 == (this.points[0]))) {
			this.neighbors[1] = t
			return
		}
		if ((p1 == (this.points[0]) && p2 == (this.points[1])) || (p1 == (this.points[1]) && p2 == (this.points[0]))) {
			this.neighbors[2] = t
			return
		}
		throw Error("Invalid markNeighbor call (1)!")
	}

	fun markNeighborTriangle(that: Triangle): Unit {
		// exhaustive search to update neighbor pointers
		if (that.containsEdgePoints(this.points[1], this.points[2])) {
			this.neighbors[0] = that
			that.markNeighbor(this, this.points[1], this.points[2])
			return
		}

		if (that.containsEdgePoints(this.points[0], this.points[2])) {
			this.neighbors[1] = that
			that.markNeighbor(this, this.points[0], this.points[2])
			return
		}

		if (that.containsEdgePoints(this.points[0], this.points[1])) {
			this.neighbors[2] = that
			that.markNeighbor(this, this.points[0], this.points[1])
			return
		}
	}

	/*public fun getPointIndexOffset(p:Point2d, offset:Int = 0):uint {
		for (var n:uint = 0; n < 3; n++) if (p == (this.points[n])) return (n + offset) % 3;
		throw(Error("Point2d not in triangle"));
	}*/

	// Optimized?
	fun getPointIndexOffset(p: Point2d, offset: Int = 0): Int {
		var no: Int = offset
		for (n in 0 until 3) {
			while (no < 0) no += 3
			while (no > 2) no -= 3
			if (p == (this.points[n])) return no
			no++
		}
		throw Error("Point2d not in triangle")
	}

	/**
	 * Alias for containsPoint
	 *
	 * @param    p
	 * @return
	 */
	fun isPointAVertex(p: Point2d): Boolean = containsPoint(p)
	//for (var n:uint = 0; n < 3; n++) if (p == [this.points[n]]) return true;
	//return false;

	/**
	 * Return the point clockwise to the given point.
	 * Return the point counter-clockwise to the given point.
	 *
	 * Return the neighbor clockwise to given point.
	 * Return the neighbor counter-clockwise to given point.
	 */

	//private const CCW_OFFSET:Int = +1;
	//private const CW_OFFSET:Int = -1;

	fun pointCW(p: Point2d): Point2d = this.points[getPointIndexOffset(p, CCW_OFFSET)]

	fun pointCCW(p: Point2d): Point2d = this.points[getPointIndexOffset(p, CW_OFFSET)]
	fun neighborCW(p: Point2d): Triangle? = this.neighbors[getPointIndexOffset(p, CW_OFFSET)]
	fun neighborCCW(p: Point2d): Triangle? = this.neighbors[getPointIndexOffset(p, CCW_OFFSET)]

	fun getConstrainedEdgeCW(p: Point2d): Boolean = this.constrained_edge[getPointIndexOffset(p, CW_OFFSET)]
	fun setConstrainedEdgeCW(p: Point2d, ce: Boolean): Boolean = ce.also { this.constrained_edge[getPointIndexOffset(p, CW_OFFSET)] = ce }
	fun getConstrainedEdgeCCW(p: Point2d): Boolean = this.constrained_edge[getPointIndexOffset(p, CCW_OFFSET)]
	fun setConstrainedEdgeCCW(p: Point2d, ce: Boolean): Boolean = ce.also { this.constrained_edge[getPointIndexOffset(p, CCW_OFFSET)] = ce }
	fun getDelaunayEdgeCW(p: Point2d): Boolean = this.delaunay_edge[getPointIndexOffset(p, CW_OFFSET)]
	fun setDelaunayEdgeCW(p: Point2d, e: Boolean): Boolean = e.also { this.delaunay_edge[getPointIndexOffset(p, CW_OFFSET)] = e }
	fun getDelaunayEdgeCCW(p: Point2d): Boolean = this.delaunay_edge[getPointIndexOffset(p, CCW_OFFSET)]
	fun setDelaunayEdgeCCW(p: Point2d, e: Boolean): Boolean = e.also { this.delaunay_edge[getPointIndexOffset(p, CCW_OFFSET)] = e }

	/**
	 * The neighbor across to given point.
	 */
	fun neighborAcross(p: Point2d): Triangle? = this.neighbors[getPointIndexOffset(p, 0)]

	fun oppositePoint(t: Triangle, p: Point2d): Point2d = this.pointCW(t.pointCW(p))

	/**
	 * Legalize triangle by rotating clockwise.<br>
	 * This method takes either 1 parameter (then the triangle is rotated around
	 * points(0)) or 2 parameters (then the triangle is rotated around the first
	 * parameter).
	 */
	fun legalize(opoint: Point2d, npoint: Point2d? = null): Unit {
		if (npoint == null) return this.legalize(this.points[0], opoint)

		if (opoint == this.points[0]) {
			this.points[1] = this.points[0]
			this.points[0] = this.points[2]
			this.points[2] = npoint
		} else if (opoint == this.points[1]) {
			this.points[2] = this.points[1]
			this.points[1] = this.points[0]
			this.points[0] = npoint
		} else if (opoint == this.points[2]) {
			this.points[0] = this.points[2]
			this.points[2] = this.points[1]
			this.points[1] = npoint
		} else {
			throw Error("Invalid js.poly2tri.Triangle.Legalize call!")
		}
	}

	/**
	 * Alias for getPointIndexOffset
	 *
	 * @param    p
	 */
	// @TODO: Do not use exceptions
	fun index(p: Point2d): Int = try {
		this.getPointIndexOffset(p, 0)
	} catch (e: Throwable) {
		-1
	}

	fun edgeIndex(p1: Point2d, p2: Point2d): Int {
		if (p1 == this.points[0]) {
			if (p2 == this.points[1]) return 2
			if (p2 == this.points[2]) return 1
		} else if (p1 == this.points[1]) {
			if (p2 == this.points[2]) return 0
			if (p2 == this.points[0]) return 2
		} else if (p1 == this.points[2]) {
			if (p2 == this.points[0]) return 1
			if (p2 == this.points[1]) return 0
		}
		return -1
	}


	/**
	 * Mark an edge of this triangle as constrained.<br>
	 * This method takes either 1 parameter (an edge index or an Edge instance) or
	 * 2 parameters (two Point2d instances defining the edge of the triangle).
	 */
	fun markConstrainedEdgeByIndex(index: Int): Unit = run { this.constrained_edge[index] = true }

	fun markConstrainedEdgeByEdge(edge: Edge): Unit = this.markConstrainedEdgeByPoints(edge.p, edge.q)

	fun markConstrainedEdgeByPoints(p: Point2d, q: Point2d): Unit {
		if ((q == (this.points[0]) && p == (this.points[1])) || (q == (this.points[1]) && p == (this.points[0]))) {
			this.constrained_edge[2] = true
		} else if ((q == (this.points[0]) && p == (this.points[2])) || (q == (this.points[2]) && p == (this.points[0]))) {
			this.constrained_edge[1] = true
		} else if ((q == (this.points[1]) && p == (this.points[2])) || (q == (this.points[2]) && p == (this.points[1]))) {
			this.constrained_edge[0] = true
		}
	}

	// isEdgeSide
	/**
	 * Checks if a side from this triangle is an edge side.
	 * If sides are not marked they will be marked.
	 *
	 * @param    ep
	 * @param    eq
	 * @return
	 */
	fun isEdgeSide(ep: Point2d, eq: Point2d): Boolean {
		val index = this.edgeIndex(ep, eq)
		if (index == -1) return false
		this.markConstrainedEdgeByIndex(index)
		this.neighbors[index]?.markConstrainedEdgeByPoints(ep, eq)
		return true
	}

	fun clearNeigbors(): Unit {
		this.neighbors[0] = null
		this.neighbors[1] = null
		this.neighbors[2] = null
	}

	fun clearDelunayEdges(): Unit {
		this.delaunay_edge[0] = false
		this.delaunay_edge[1] = false
		this.delaunay_edge[2] = false
	}

	override fun equals(other: Any?): Boolean = (other is Triangle) && (this.p1 == other.p1) && (this.p2 == other.p2) && (this.p3 == other.p3)

	fun pointInsideTriangle(pp: Point2d): Boolean {
		val p1: Point2d = points[0]
		val p2: Point2d = points[1]
		val p3: Point2d = points[2]
		if (_product(p1, p2, p3) >= 0) {
			return (_product(p1, p2, pp) >= 0) && (_product(p2, p3, pp)) >= 0 && (_product(p3, p1, pp) >= 0)
		} else {
			return (_product(p1, p2, pp) <= 0) && (_product(p2, p3, pp)) <= 0 && (_product(p3, p1, pp) <= 0)
		}
	}

	val area: Double
		get() {
			val a = p2.x - p1.x
			val b = p2.y - p1.y

			val c = p3.x - p1.x
			val d = p3.y - p1.y

			return abs(a * d - b * c) / 2.0
		}

	override fun toString(): String = "Triangle(${this.points[0]}, ${this.points[1]}, ${this.points[2]})"

	companion object {
		private const val CW_OFFSET: Int = +1
		private const val CCW_OFFSET: Int = -1

		fun getNotCommonVertexIndex(t1: Triangle, t2: Triangle): Int {
			var sum: Int = 0
			var index: Int = -1
			if (!t2.containsPoint(t1.points[0])) {
				index = 0
				sum++
			}
			if (!t2.containsPoint(t1.points[1])) {
				index = 1
				sum++
			}
			if (!t2.containsPoint(t1.points[2])) {
				index = 2
				sum++
			}
			if (sum != 1) throw Error("Triangles are not contiguous")
			return index
		}

		fun getNotCommonVertex(t1: Triangle, t2: Triangle): Point2d = t1.points[getNotCommonVertexIndex(t1, t2)]

		fun getCommonEdge(t1: Triangle, t2: Triangle): Edge {
			val commonIndexes = ArrayList<Point2d>()
			for (point in t1.points) if (t2.containsPoint(point)) commonIndexes.add(point)
			if (commonIndexes.size != 2) throw Error("Triangles are not contiguous")
			return t1.ctx.createEdge(commonIndexes[0], commonIndexes[1])
		}


		/**
		 * Rotates a triangle pair one vertex CW
		 *<pre>
		 *       n2                    n2
		 *  P +-----+             P +-----+
		 *    | t  /|               |\  t |
		 *    |   / |               | \   |
		 *  n1|  /  |n3           n1|  \  |n3
		 *    | /   |    after CW   |   \ |
		 *    |/ oT |               | oT \|
		 *    +-----+ oP            +-----+
		 *       n4                    n4
		 * </pre>
		 */
		fun rotateTrianglePair(t: Triangle, p: Point2d, ot: Triangle, op: Point2d): Unit {
			val n1 = t.neighborCCW(p)
			val n2 = t.neighborCW(p)
			val n3 = ot.neighborCCW(op)
			val n4 = ot.neighborCW(op)

			val ce1 = t.getConstrainedEdgeCCW(p)
			val ce2 = t.getConstrainedEdgeCW(p)
			val ce3 = ot.getConstrainedEdgeCCW(op)
			val ce4 = ot.getConstrainedEdgeCW(op)

			val de1 = t.getDelaunayEdgeCCW(p)
			val de2 = t.getDelaunayEdgeCW(p)
			val de3 = ot.getDelaunayEdgeCCW(op)
			val de4 = ot.getDelaunayEdgeCW(op)

			t.legalize(p, op)
			ot.legalize(op, p)

			// Remap delaunay_edge
			ot.setDelaunayEdgeCCW(p, de1)
			t.setDelaunayEdgeCW(p, de2)
			t.setDelaunayEdgeCCW(op, de3)
			ot.setDelaunayEdgeCW(op, de4)

			// Remap constrained_edge
			ot.setConstrainedEdgeCCW(p, ce1)
			t.setConstrainedEdgeCW(p, ce2)
			t.setConstrainedEdgeCCW(op, ce3)
			ot.setConstrainedEdgeCW(op, ce4)

			// Remap neighbors
			// XXX: might optimize the markNeighbor by keeping track of
			//      what side should be assigned to what neighbor after the
			//      rotation. Now mark neighbor does lots of testing to find
			//      the right side.
			t.clearNeigbors()
			ot.clearNeigbors()
			if (n1 != null) ot.markNeighborTriangle(n1)
			if (n2 != null) t.markNeighborTriangle(n2)
			if (n3 != null) t.markNeighborTriangle(n3)
			if (n4 != null) ot.markNeighborTriangle(n4)
			t.markNeighborTriangle(ot)
		}

		fun getUniquePointsFromTriangles(triangles: ArrayList<Triangle>) = triangles.flatMap { it.points.toList() }.distinct()

		fun traceList(triangles: ArrayList<Triangle>): Unit {
			val pointsList = Triangle.getUniquePointsFromTriangles(triangles)
			val pointsMap = hashMapOf<Point2d, Int>()
			var points_length: Int = 0
			for (point in pointsList) pointsMap[point] = ++points_length
			fun getPointName(point: Point2d): String = "p" + pointsMap[point]
			println("Points:")
			for (point in pointsList) println("  " + getPointName(point) + " = " + point)
			println("Triangles:")
			for (triangle in triangles) println("  Triangle(${getPointName(triangle.points[0])}, ${getPointName(triangle.points[1])}, ${getPointName(triangle.points[2])})")
		}

		private fun _product(p1: Point2d, p2: Point2d, p3: Point2d): Double = (p1.x - p3.x) * (p2.y - p3.y) - (p1.y - p3.y) * (p2.x - p3.x)

		/**
		 * <b>Requirement</b>:<br>
		 * 1. a, b and c form a triangle.<br>
		 * 2. a and d is know to be on opposite side of bc<br>
		 * <pre>
		 *                a
		 *                +
		 *               / \
		 *              /   \
		 *            b/     \c
		 *            +-------+
		 *           /    d    \
		 *          /           \
		 * </pre>
		 * <b>Fact</b>: d has to be in area B to have a chance to be inside the circle formed by
		 *  a,b and c<br>
		 *  d is outside B if orient2d(a,b,d) or orient2d(c,a,d) is CW<br>
		 *  This preknowledge gives us a way to optimize the incircle test
		 * @param pa - triangle point, opposite d
		 * @param pb - triangle point
		 * @param pc - triangle point
		 * @param pd - point opposite a
		 * @return true if d is inside circle, false if on circle edge
		 */
		fun insideIncircle(pa: Point2d, pb: Point2d, pc: Point2d, pd: Point2d): Boolean {
			val adx: Double = pa.x - pd.x
			val ady: Double = pa.y - pd.y
			val bdx: Double = pb.x - pd.x
			val bdy: Double = pb.y - pd.y

			val adxbdy: Double = adx * bdy
			val bdxady: Double = bdx * ady
			val oabd: Double = adxbdy - bdxady

			if (oabd <= 0) return false

			val cdx: Double = pc.x - pd.x
			val cdy: Double = pc.y - pd.y

			val cdxady: Double = cdx * ady
			val adxcdy: Double = adx * cdy
			val ocad: Double = cdxady - adxcdy

			if (ocad <= 0) return false

			val bdxcdy: Double = bdx * cdy
			val cdxbdy: Double = cdx * bdy

			val alift: Double = adx * adx + ady * ady
			val blift: Double = bdx * bdx + bdy * bdy
			val clift: Double = cdx * cdx + cdy * cdy

			val det: Double = alift * (bdxcdy - cdxbdy) + blift * ocad + clift * oabd
			return det > 0
		}

		fun inScanArea(pa: Point2d, pb: Point2d, pc: Point2d, pd: Point2d): Boolean {
			val pdx: Double = pd.x
			val pdy: Double = pd.y
			val adx: Double = pa.x - pdx
			val ady: Double = pa.y - pdy
			val bdx: Double = pb.x - pdx
			val bdy: Double = pb.y - pdy

			val adxbdy: Double = adx * bdy
			val bdxady: Double = bdx * ady
			val oabd: Double = adxbdy - bdxady

			if (oabd <= Constants.EPSILON) return false

			val cdx: Double = pc.x - pdx
			val cdy: Double = pc.y - pdy

			val cdxady: Double = cdx * ady
			val adxcdy: Double = adx * cdy
			val ocad: Double = cdxady - adxcdy

			if (ocad <= Constants.EPSILON) return false

			return true
		}
	}
}

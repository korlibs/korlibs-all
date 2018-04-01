package com.soywiz.korma.geom.triangle

import com.soywiz.kds.PriorityQueue
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.math.Math

data class FunnelPortal(var left: Point2d, var right: Point2d)

class NewFunnel {
	private val portals = ArrayList<Portal>()
	var path = arrayListOf<Point2d>()

	companion object {
		protected fun triarea2(a: Point2d, b: Point2d, c: Point2d): Double {
			val ax = b.x - a.x
			val ay = b.y - a.y
			val bx = c.x - a.x
			val by = c.y - a.y
			return bx * ay - ax * by
		}

		protected fun vdistsqr(a: Point2d, b: Point2d): Double {
			return Math.hypot(b.x - a.x, b.y - a.y)
		}

		protected fun vequal(a: Point2d, b: Point2d): Boolean {
			return vdistsqr(a, b) < (0.001 * 0.001)
		}
	}


	fun push(p1: Point2d, p2: Point2d = p1): Unit {
		this.portals.add(Portal(p1, p2))
		/*if (p2 == p1) {
			trace('channel.push(' + p1 + ');');
		} else {
			trace('channel.push(' + p1 + ', ' + p2 + ');');
		}*/
	}

	fun stringPull(): ArrayList<Point2d> {
		val pts = ArrayList<Point2d>()
		// Init scan state
		var portalApex: Point2d
		var portalLeft: Point2d
		var portalRight: Point2d
		var apexIndex: Int = 0
		var leftIndex: Int = 0
		var rightIndex: Int = 0

		portalApex = portals[0].left
		portalLeft = portals[0].left
		portalRight = portals[0].right

		// Add start point.
		pts.add(portalApex)

		var i = 0
		while (i < portals.size) {
			val left = portals[i].left
			val right = portals[i].right
			i++

			// Update right vertex.
			if (triarea2(portalApex, portalRight, right) <= 0.0) {
				if (vequal(portalApex, portalRight) || triarea2(portalApex, portalLeft, right) > 0.0) {
					// Tighten the funnel.
					portalRight = right
					rightIndex = i
				} else {
					// Right over left, insert left to path and restart scan from portal left point.
					pts.add(portalLeft)
					// Make current left the apex.
					portalApex = portalLeft
					apexIndex = leftIndex
					// Reset portal
					portalLeft = portalApex
					portalRight = portalApex
					leftIndex = apexIndex
					rightIndex = apexIndex
					// Restart scan
					i = apexIndex
					continue
				}
			}

			// Update left vertex.
			if (triarea2(portalApex, portalLeft, left) >= 0.0) {
				if (vequal(portalApex, portalLeft) || triarea2(portalApex, portalRight, left) < 0.0) {
					// Tighten the funnel.
					portalLeft = left
					leftIndex = i
				} else {
					// Left over right, insert right to path and restart scan from portal right point.
					pts.add(portalRight)
					// Make current right the apex.
					portalApex = portalRight
					apexIndex = rightIndex
					// Reset portal
					portalLeft = portalApex
					portalRight = portalApex
					leftIndex = apexIndex
					rightIndex = apexIndex
					// Restart scan
					i = apexIndex
					continue
				}
			}
		}

		if ((pts.size == 0) || (!vequal(pts[pts.size - 1], portals[portals.size - 1].left))) {
			// Append last point to path.
			pts.add(portals[portals.size - 1].left)
		}

		this.path = pts
		return pts
	}

	data class Portal(val left: Point2d, val right: Point2d)
}

class PathFind(val spatialMesh: SpatialMesh) {
	protected var openedList = PriorityQueue<SpatialNode> { l, r -> l.F.compareTo(r.F) }

	init {
		reset()
	}

	protected fun reset(): Unit {
		openedList = PriorityQueue<SpatialNode> { l: SpatialNode, r: SpatialNode -> l.F.compareTo(r.F) }
		for (node in this.spatialMesh.nodes) {
			node.parent = null
			node.G = 0
			node.H = 0
			node.closed = false
		}
	}

	fun find(startNode: SpatialNode?, endNode: SpatialNode?): List<SpatialNode> {
		val returnList = ArrayList<SpatialNode>()
		reset()
		var currentNode = startNode

		if (startNode !== null && endNode !== null) {
			addToOpenedList(startNode)

			while ((currentNode != endNode) && openedListHasItems()) {
				currentNode = getAndRemoveFirstFromOpenedList()
				addNodeToClosedList(currentNode)

				for (neighborNode in getNodeNeighbors(currentNode)) {
					// Ignore invalid paths and the ones on the closed list.
					if (neighborNode === null) continue
					if (inClosedList(neighborNode)) continue

					val G = currentNode.G + neighborNode.distanceToSpatialNode(currentNode)
					// Not in opened list yet.
					if (!inOpenedList(neighborNode)) {
						addToOpenedList(neighborNode)
						neighborNode.G = G
						neighborNode.H = neighborNode.distanceToSpatialNode(endNode)
						neighborNode.parent = currentNode
						updatedNodeOnOpenedList(neighborNode)
					}
					// In opened list but with a worse G than this one.
					else if (G < neighborNode.G) {
						neighborNode.G = G
						neighborNode.parent = currentNode
						updatedNodeOnOpenedList(neighborNode)
					}
				}
			}
		}

		if (currentNode != endNode) throw(PathFindException("Can't find a path", 1))

		while (currentNode != startNode) {
			returnList.add(currentNode!!)
			//returnList.push(currentNode);
			currentNode = currentNode.parent
		}

		if (startNode != null) returnList.add(startNode)

		returnList.reverse()

		return returnList
	}

	protected fun addToOpenedList(node: SpatialNode): Unit = run { openedList.push(node) }
	protected fun openedListHasItems(): Boolean = openedList.length > 0
	protected fun getAndRemoveFirstFromOpenedList(): SpatialNode = openedList.removeHead()
	protected fun addNodeToClosedList(node: SpatialNode): Unit = run { node.closed = true }
	protected fun inClosedList(node: SpatialNode): Boolean = node.closed
	protected fun getNodeNeighbors(node: SpatialNode): Array<SpatialNode?> = node.neighbors
	protected fun inOpenedList(node: SpatialNode): Boolean = openedList.contains(node)

	protected fun updatedNodeOnOpenedList(node: SpatialNode): Unit {
		openedList.updateObject(node)
	}

	/*public fun inClosedList(node:SpatialNode):Boolean {
		return (closedList[node] !== undefined);
	}*/
}

object PathFindChannel {
	fun channelToPortals(startPoint: Point2d, endPoint: Point2d, channel: List<SpatialNode>): NewFunnel {
		val portals = NewFunnel()

		portals.push(startPoint)

		if (channel.size >= 2) {
			val firstTriangle = channel[0].triangle!!
			val secondTriangle = channel[1].triangle!!
			val lastTriangle = channel[channel.size - 1].triangle!!

			assert(firstTriangle.pointInsideTriangle(startPoint))
			assert(lastTriangle.pointInsideTriangle(endPoint))

			val startVertex = Triangle.getNotCommonVertex(firstTriangle, secondTriangle)

			var vertexCW0: Point2d = startVertex
			var vertexCCW0: Point2d = startVertex

			//trace(startVertex);

			for (n in 0 until channel.size - 1) {
				val triangleCurrent = channel[n + 0].triangle!!
				val triangleNext = channel[n + 1].triangle!!
				val commonEdge = Triangle.getCommonEdge(triangleCurrent, triangleNext)
				val vertexCW1 = triangleCurrent.pointCW(vertexCW0)
				val vertexCCW1 = triangleCurrent.pointCCW(vertexCCW0)
				if (!commonEdge.hasPoint(vertexCW0)) vertexCW0 = vertexCW1
				if (!commonEdge.hasPoint(vertexCCW0)) vertexCCW0 = vertexCCW1
				portals.push(vertexCW0, vertexCCW0)
				//trace(vertexCW0, vertexCCW0);
			}
		}

		portals.push(endPoint)

		portals.stringPull()

		return portals
	}

	fun channelToPortals2(startPoint: Point2d, endPoint: Point2d, channel: ArrayList<SpatialNode>): NewFunnel {
		/*
		var nodeStart:SpatialNode   = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(Point2d(50, 50)));
		//var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(Point2d(73, 133)));
		//var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(Point2d(191, 152)));
		//var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(Point2d(316, 100)));
		var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(Point2d(300, 300)));
		channel[0].triangle.pointInsideTriangle();
		channel[0].triangle.points[0]
		*/

		val portals = NewFunnel()
		val firstTriangle = channel[0].triangle!!
		val secondTriangle = channel[1].triangle!!
		val lastTriangle = channel[channel.size - 1].triangle!!

		assert(firstTriangle.pointInsideTriangle(startPoint))
		assert(lastTriangle.pointInsideTriangle(endPoint))

		val startVertexIndex = Triangle.getNotCommonVertexIndex(firstTriangle, secondTriangle)
		//firstTriangle.containsPoint(firstTriangle.points[0]);

		// Add portals.

		var currentVertexCW: Point2d = firstTriangle.points[startVertexIndex]
		var currentVertexCCW: Point2d = firstTriangle.points[startVertexIndex]
		//var currentTriangle:Triangle = firstTriangle;

		portals.push(startPoint)

		for (n in 1 until channel.size) {
			val edge = Triangle.getCommonEdge(channel[n - 1].triangle!!, channel[n].triangle!!)
			portals.push(edge.p, edge.q)
			//trace(edge);
		}

		/*
		for (var n:uint = 0; n < channel.length; n++) {
			trace(currentVertexCW + " | " + currentVertexCCW);
			currentVertexCW = channel[n].triangle.pointCW(currentVertexCW);
			currentVertexCCW = channel[n].triangle.pointCCW(currentVertexCCW);
			portals.push(FunnelPortal(currentVertexCW, currentVertexCCW));
			//firstTriangle.pointCW();
		}
		*/

		portals.push(endPoint)

		portals.stringPull()

		return portals
	}

	private fun assert(test: Boolean): Unit {
		if (!test) throw(Error("Assert error"))
	}
}

class PathFindException(message: String = "", val index: Int = 0) : Error(message)

class SpatialMesh() {
	protected var mapTriangleToSpatialNode = hashMapOf<Triangle, SpatialNode>()
	var nodes = arrayListOf<SpatialNode>()

	constructor(triangles: Iterable<Triangle>) : this() {
		for (triangle in triangles) {
			val node = getNodeFromTriangle(triangle)
			if (node != null) nodes.add(node)
		}
	}

	fun spatialNodeFromPoint(point: Point2d): SpatialNode {
		for (node in nodes) {
			if (node.triangle!!.pointInsideTriangle(point)) return node
		}
		throw Error("Point2d not inside triangles")
	}

	fun getNodeAt(point: Point2d): SpatialNode? {
		for (node in nodes) if (node.triangle!!.containsPoint(point)) return node
		return null
	}

	fun getNodeFromTriangle(triangle: Triangle?): SpatialNode? {
		if (triangle === null) return null

		if (!mapTriangleToSpatialNode.containsKey(triangle)) {
			val tp = triangle.points
			val sn = SpatialNode(
				x = ((tp[0].x + tp[1].x + tp[2].x) / 3).toInt().toDouble(),
				y = ((tp[0].y + tp[1].y + tp[2].y) / 3).toInt().toDouble(),
				z = 0.0,
				triangle = triangle,
				G = 0,
				H = 0
			)
			mapTriangleToSpatialNode[triangle] = sn
			sn.neighbors = arrayOf(if (triangle.constrained_edge[0]) null else getNodeFromTriangle(triangle.neighbors[0]),
				if (triangle.constrained_edge[1]) null else getNodeFromTriangle(triangle.neighbors[1]),
				if (triangle.constrained_edge[2]) null else getNodeFromTriangle(triangle.neighbors[2])
			)
		}
		return mapTriangleToSpatialNode[triangle]
	}


	companion object {
		fun fromTriangles(triangles: Iterable<Triangle>): SpatialMesh = SpatialMesh(triangles)
	}

	override fun toString() = "SpatialMesh(" + nodes.toString() + ")"
}

class SpatialNode(
	var x: Double = 0.0,
	var y: Double = 0.0,
	var z: Double = 0.0,
	var triangle: Triangle? = null,
	var G: Int = 0, // Cost
	var H: Int = 0, // Heuristic
	var neighbors: Array<SpatialNode?> = arrayOfNulls<SpatialNode>(3),
	var parent: SpatialNode? = null,
	var closed: Boolean = false
) {
	val F: Int get() = G + H // F = G + H

	fun distanceToSpatialNode(that: SpatialNode): Int = Math.hypot(this.x - that.x, this.y - that.y).toInt()

	override fun toString(): String = "SpatialNode($x, $y)"
}


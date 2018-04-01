package com.soywiz.korma.geom.triangle

import com.soywiz.korma.assertEqualsDouble
import com.soywiz.kds.PriorityQueue
import com.soywiz.korma.geom.Orientation
import com.soywiz.korma.geom.Point2d
import org.junit.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EdgeTest {
	private var p1: Point2d = Point2d(0, 0)
	private var p2: Point2d = Point2d(-1, 0)
	private var e1: Edge = EdgeContext().createEdge(p1, p2)

	@Test
	fun testInstantiated(): Unit {
		assertTrue(e1 is Edge)
	}

	@Test
	fun testValues(): Unit {
		// Order of points was reversed.
		assertEquals(e1.p, p2)
		assertEquals(e1.q, p1)
	}

	@Test
	fun testInEdgeList(): Unit {
		// q contains the edge_list
		assertTrue(e1.ctx.getPointEdgeList(e1.q).indexOf(e1) != -1)
	}
}

class NewFunnelTest {
	@Test
	fun testStringPull(): Unit {
		val channel = NewFunnel()

		channel.push(Point2d(1, 0))
		channel.push(Point2d(0, 4), Point2d(4, 3))
		channel.push(Point2d(4, 7), Point2d(4, 3))
		channel.push(Point2d(16, 0), Point2d(10, 1))
		channel.push(Point2d(16, 0), Point2d(9, -5))
		channel.push(Point2d(12, -11))
		channel.stringPull()

		assertEquals("[Vector2(1, 0), Vector2(4, 3), Vector2(10, 1), Vector2(12, -11)]", channel.path.toString())
	}

}

class OrientationTest {
	@Test
	fun testOrientationCollinear(): Unit {
		assertEquals(Orientation.Companion.orient2d(Point2d(0, 0), Point2d(1, 1), Point2d(2, 2)), Orientation.COLLINEAR)
		assertFalse(Orientation.Companion.orient2d(Point2d(-1, 0), Point2d(0, 0), Point2d(+1, 0)) != Orientation.COLLINEAR)
	}

	@Test
	fun testOrientationCW(): Unit {
		assertEquals(Orientation.Companion.orient2d(Point2d(0, 0), Point2d(1, 1), Point2d(2, 0)), Orientation.CW)
	}

	@Test
	fun testOrientationCCW(): Unit {
		assertEquals(Orientation.Companion.orient2d(Point2d(0, 0), Point2d(-1, 1), Point2d(-2, 0)), Orientation.CCW)
	}
}

//class PathFindTest {
//	protected var vp:VisiblePolygon;
//	protected var spatialMesh:SpatialMesh;
//	protected var pathNodes:ArrayList<SpatialNode>;
//	protected var pathFind:PathFind;
//
//	val triangles get() = vp.triangles
//
//	init {
//		vp = VisiblePolygon();
//		vp.addRectangle(0, 0, 400, 400);
//		vp.addRectangle(200, 100, 80, 80);
//		vp.addRectangle(100, 200, 80, 80);
//
//		//vp.addRectangle(110, 210, 60, 60);
//
//		vp.addRectangle(100, 20, 80, 150);
//		vp.addRectangle(37, 140, 37, 104);
//		var vec = ArrayList<Point2d>();
//		vec.push(Point2d(10, 10));
//		vec.push(Point2d(40, 10));
//		vec.push(Point2d(60, 20));
//		vec.push(Point2d(40, 40));
//		vec.push(Point2d(10, 40));
//		vp.addPolyline(vec);
//		spatialMesh = SpatialMesh.fromTriangles(triangles);
//		pathFind = PathFind(spatialMesh);
//	}
//
//	fun debugDrawPath():Unit {
//		var mc:MovieClip = MovieClip();
//		mc.graphics.clear();
//		vp.drawShape(mc.graphics);
//		for each (var node:SpatialNode in pathNodes) {
//		VisiblePolygon.drawTriangleHighlight(node.triangle, mc.graphics);
//	}
//		/*
//		VisiblePolygon.drawTriangleHighlight(nodeStart.triangle, mc.graphics);
//		VisiblePolygon.drawTriangleHighlight(nodeEnd.triangle, mc.graphics);
//		*/
//		this.context.addChild(mc);
//		context.stage.addEventListener(MouseEvent.CLICK, onClick);
//	}
//
//	private fun onClick(e:MouseEvent):Unit {
//		//trace(TrianglesToSpatialNodesConverter.convert(triangles));
//
//		/*
//		var pointStart:Point2d = Point2d(50, 50);
//		var pointEnd:Point2d = Point2d(340, 200);
//		//var pointEnd:Point2d = Point2d(160, 260);
//		var nodeStart:SpatialNode   = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointStart));
//		var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointEnd));
//
//		pathNodes = pathFind.find(nodeStart, nodeEnd);
//
//		assertTrue(pathNodes.toString(), "SpatialNode(45, 53),SpatialNode(70, 100),SpatialNode(91, 110),SpatialNode(82, 184),SpatialNode(91, 204),SpatialNode(126, 180),SpatialNode(153, 190),SpatialNode(186, 183),SpatialNode(186, 220),SpatialNode(220, 213),SpatialNode(286, 286)");
//
//		var portals:NewFunnel = PathFindChannel.channelToPortals(pointStart, pointEnd, pathNodes)
//
//		debugDrawPath(); var mc:MovieClip = MovieClip(); Drawing.drawLines(mc, portals.path); addChild(mc);
//
//		assertEquals("Point2d(50, 50),Point2d(100, 170),Point2d(180, 200),Point2d(300, 300)", portals.path);
//		*/
//
//		var pointStart:Point2d = Point2d(50, 50);
//		var pointEnd:Point2d = Point2d(300, 300);
//
//		var nodeStart:SpatialNode   = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointStart));
//		var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointEnd));
//
//		pathNodes = pathFind.find(nodeStart, nodeEnd);
//
//		var portals:NewFunnel = PathFindChannel.channelToPortals(pointStart, pointEnd, pathNodes)
//
//		debugDrawPath(); var mc:MovieClip = MovieClip(); Drawing.drawLines(mc, portals.path); addChild(mc);
//	}
//
//	fun testDemo():Unit {
//
//		//trace(TrianglesToSpatialNodesConverter.convert(triangles));
//
//		var pointStart:Point2d = Point2d(50, 50);
//		var pointEnd:Point2d = Point2d(300, 300);
//
//		var nodeStart:SpatialNode   = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointStart));
//		var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointEnd));
//
//		pathNodes = pathFind.find(nodeStart, nodeEnd);
//
//		assertTrue(pathNodes.toString(), "SpatialNode(45, 53),SpatialNode(70, 100),SpatialNode(91, 110),SpatialNode(82, 184),SpatialNode(91, 204),SpatialNode(126, 180),SpatialNode(153, 190),SpatialNode(186, 183),SpatialNode(186, 220),SpatialNode(220, 213),SpatialNode(286, 286)");
//
//		var portals:NewFunnel = PathFindChannel.channelToPortals(pointStart, pointEnd, pathNodes)
//
//		//debugDrawPath(); var mc:MovieClip = MovieClip(); Drawing.drawLines(mc, portals.path); addChild(mc);
//
//		assertEquals("Point2d(50, 50),Point2d(100, 170),Point2d(180, 200),Point2d(300, 300)", portals.path);
//	}
//}

class PointTest {
	companion object {
		private const val P1X: Double = 1.0
		private const val P1Y: Double = 3.0
		private const val P2X: Double = 7.0
		private const val P2Y: Double = 2.0
		private const val P3X: Double = 5.0
		private const val P3Y: Double = 5.0
		private const val SCALAR: Double = 3.0
		private const val DELTA_FLOAT: Double = 0.0001
	}

	private var p1: Point2d
	private var p2: Point2d
	private var p3: Point2d

	init {
		p1 = Point2d(P1X, P1Y)
		p2 = Point2d(P2X, P2Y)
		p3 = Point2d(P3X, P3Y)
	}

	@Test
	fun testInstantiated(): Unit {
		assertTrue(p1 is Point2d)
	}

	@Test
	fun testValues(): Unit {
		assertEqualsDouble(p1.x, P1X, DELTA_FLOAT)
		assertEqualsDouble(p1.y, P1Y, DELTA_FLOAT)
	}

	@Test
	fun testSum(): Unit {
		p1.add(p2)
		assertEqualsDouble(p1.x, P1X + P2X, DELTA_FLOAT)
		assertEqualsDouble(p1.y, P1Y + P2Y, DELTA_FLOAT)
	}

	@Test
	fun testSub(): Unit {
		p1.sub(p2)
		assertEqualsDouble(p1.x, P1X - P2X, DELTA_FLOAT)
		assertEqualsDouble(p1.y, P1Y - P2Y, DELTA_FLOAT)
	}

	@Test
	fun testNeg(): Unit {
		p1.neg()
		assertEqualsDouble(p1.x, -P1X, DELTA_FLOAT)
		assertEqualsDouble(p1.y, -P1Y, DELTA_FLOAT)
	}

	@Test
	fun testMul(): Unit {
		p1.mul(SCALAR)
		assertEqualsDouble(p1.x, P1X * SCALAR, DELTA_FLOAT)
		assertEqualsDouble(p1.y, P1Y * SCALAR, DELTA_FLOAT)
	}

	@Test
	fun testLength(): Unit {
		assertEqualsDouble(p1.length, sqrt(P1X * P1X + P1Y * P1Y), DELTA_FLOAT)
	}

	@Test
	fun testNormalize(): Unit {
		assertNotEquals(p3.length, 1.0)
		p3.normalize(); assertEqualsDouble(p3.length, 1.0, DELTA_FLOAT)
		p1.normalize(); assertEqualsDouble(p1.length, 1.0, DELTA_FLOAT)
		p2.normalize(); assertEqualsDouble(p2.length, 1.0, DELTA_FLOAT)
	}

	@Test
	fun testEquals(): Unit {
		assertTrue(p1 == p1)
		assertTrue(p1 == Point2d(P1X, P1Y))
		assertFalse(p1 == p2)
	}

	@Test
	fun testToString(): Unit {
		assertEquals(Point2d(P2X, P2Y), p2)
	}
}

class PriorityQueueTest {

	// Items inserted on setUp.
	private val o1 = QueueItem("o1", 1)
	private val o2 = QueueItem("o2", -5)
	private val o3 = QueueItem("o3", 7)
	private val o4 = QueueItem("o4", 4)
	private val o5 = QueueItem("o5", 9)
	private val o6 = QueueItem("o6", 0)

	private val pq = PriorityQueue<QueueItem> { l, r -> l.priority.compareTo(r.priority) }.apply {
		add(o1, o2, o3, o4, o5, o6)
	}

	// Items not inserted on setUp.
	private val n1 = QueueItem("n1", 3)

	@Test
	fun testInitialOrder(): Unit {
		assertEquals("o2", pq.head.toString())
		assertEquals("[o2, o6, o1, o4, o3, o5]", pq._sortedList.toString())
	}

	@Test
	fun testUpdateOrder(): Unit {
		o3.priority = -6
		pq.updateObject(o3)
		assertEquals("o3", pq.head.toString())
		assertEquals("[o3, o2, o6, o1, o4, o5]", pq._sortedList.toString())
	}

	@Test
	fun testPostUpdateInsert(): Unit {
		o3.priority = -6
		pq.updateObject(o3)
		pq.push(n1)
		assertEquals("[o3, o2, o6, o1, n1, o4, o5]", pq._sortedList.toString())
	}

	@Test
	fun testContains(): Unit {
		assertTrue(pq.contains(o4))
		assertFalse(pq.contains(n1))
	}

	internal class QueueItem(var name: String, var priority: Int) {
		override fun toString() = name
	}

}

@Suppress("unused")
class SpatialMeshTest {
	//var points:ArrayList<Point2d>
	//var triangles:ArrayList<Triangle>
//
	//protected fun addPoint(info:String):Unit {
	//	var result = /^p(\d+):\s*(-?\d+)\s*,\s*(-?\d+)$/ig.exec(info)
	//	var pointId = parseInt(result[1])
	//	var point:Point2d = Point2d(parseInt(result[2]), parseInt(result[3]))
	//	if (points.length < pointId + 1) points.length = pointId + 1
	//	points[pointId] = point
	//}
//
	//protected fun addTriangle(info:String):Unit {
	//	var result = /^t(\d+):\s*p(\d+)\s*,\s*p(\d+)\s*,\s*p(\d+)$/ig.exec(info)
	//	var triangleId:int = parseInt(result[1])
	//	var p1:Point2d = points[parseInt(result[1 + 1])]
	//	var p2:Point2d = points[parseInt(result[1 + 2])]
	//	var p3:Point2d = points[parseInt(result[1 + 3])]
	//	var triangle:Triangle = Triangle(p1, p2, p3, true, false)
	//	if (triangles.length < triangleId + 1) triangles.length = triangleId + 1
	//	triangles[triangleId] = triangle
	//	//trace(triangleId);
	//}
//
	//protected fun addNeighbors(info:String):Unit {
	//	var tt = info.replace(/[t|\s]+/gi, '').split(':')
	//	var triangleId = parseInt(tt[0])
	//	var neighborIds:Array = tt[1].split(',').map(function(v:*, index:int, arr:Array):int { return parseInt(v); })
	//	var n:int = 0
	//	for (neighborId in neighborIds) {
	//	triangles[triangleId].neighbors[n++] = triangles[neighborId]
	//	}
//
	//}
//
	//init {
	//	points    = ArrayList<Point2d>()
	//	triangles = ArrayList<Triangle>()
//
	//	addPoint("p1: 1,  0")
	//	addPoint("p2: 2,  3")
	//	addPoint("p3: 3,  1")
	//	addPoint("p4: 4,  3")
	//	addPoint("p5: 2, -1")
	//	addPoint("p6: 4,  4")
	//	addPoint("p7: 0,  2")
//
	//	addTriangle("t1: p1, p2, p3")
	//	addTriangle("t2: p2, p3, p4")
	//	addTriangle("t3: p1, p3, p5")
	//	addTriangle("t4: p2, p6, p4")
	//	addTriangle("t5: p7, p2, p1")
//
	//	addNeighbors("t1: t2, t3, t5")
	//	addNeighbors("t2: t1, t4")
	//	addNeighbors("t3: t1")
	//	addNeighbors("t4: t2")
	//	addNeighbors("t5: t1")
	//}
//
	//@Test
	//fun testTest():Unit {
	//	val spatialMesh = SpatialMesh.fromTriangles(triangles.slice(1, triangles.size))
	//	assertTrue("SpatialMesh(SpatialNode(2, 1),SpatialNode(3, 2),SpatialNode(2, 0),SpatialNode(3, 3),SpatialNode(1, 1))", spatialMesh.toString())
	//}

}

class SweepContextTest {
	private var initialPoints = arrayListOf(Point2d(0, 0), Point2d(0, 100), Point2d(100, 100), Point2d(100, 0))
	private var holePoints = arrayListOf(Point2d(10, 10), Point2d(10, 90), Point2d(90, 90), Point2d(90, 10))
	private var sweepContext = SweepContext(initialPoints).apply { addHole(holePoints) }

	@Test
	fun testInitTriangulation(): Unit {
		assertEquals("[Vector2(0, 0), Vector2(0, 100), Vector2(100, 100), Vector2(100, 0), Vector2(10, 10), Vector2(10, 90), Vector2(90, 90), Vector2(90, 10)]", this.sweepContext.points.toString())
		this.sweepContext.initTriangulation()
		assertEquals("[Vector2(0, 0), Vector2(100, 0), Vector2(10, 10), Vector2(90, 10), Vector2(10, 90), Vector2(90, 90), Vector2(0, 100), Vector2(100, 100)]", this.sweepContext.points.toString())
	}
}

class SweepTest {
	private var initialPoints = arrayListOf(Point2d(0, 0), Point2d(100, 0), Point2d(100, 100), Point2d(0, 100))
	private var holePoints = arrayListOf(
		Point2d(10, 10),
		Point2d(10, 90),
		Point2d(90, 90),
		Point2d(90, 10)
	)
	private var sweepContext: SweepContext = SweepContext(initialPoints)
	private var sweep: Sweep = Sweep(this.sweepContext)

	@Test
	fun testBoxTriangulate(): Unit {
		//Edge.traceList(this.sweepContext.edge_list);
		this.sweep.triangulate()
		assertEquals(2, this.sweepContext.triangles.size)
		assertEquals("[Triangle(Vector2(0, 100), Vector2(100, 0), Vector2(100, 100)), Triangle(Vector2(0, 100), Vector2(0, 0), Vector2(100, 0))]", this.sweepContext.triangles.toString())
	}

	@Test
	fun testBoxWithHoleTriangulate(): Unit {
		this.sweepContext.addHole(holePoints)
		this.sweep.triangulate()
		assertEquals(8, this.sweepContext.triangles.size)
		assertEquals("[Triangle(Vector2(0, 100), Vector2(10, 90), Vector2(100, 100)), Triangle(Vector2(10, 90), Vector2(90, 90), Vector2(100, 100)), Triangle(Vector2(90, 90), Vector2(100, 0), Vector2(100, 100)), Triangle(Vector2(90, 90), Vector2(90, 10), Vector2(100, 0)), Triangle(Vector2(90, 10), Vector2(10, 10), Vector2(100, 0)), Triangle(Vector2(10, 10), Vector2(0, 0), Vector2(100, 0)), Triangle(Vector2(0, 0), Vector2(10, 10), Vector2(10, 90)), Triangle(Vector2(0, 100), Vector2(0, 0), Vector2(10, 90))]", this.sweepContext.triangles.toString())
	}
}

class TriangleTest {
	/**
	 *    p5
	 *     |\
	 *     | \
	 *     |   \ p4
	 *   p3|____\____.p6
	 *    / \ t2|   /
	 *   / t1\  |  /
	 *  /_____\.|/
	 * p1      p2
	 *
	 */
	private val p1 = Point2d(-1, 0)
	private val p2 = Point2d(+1, 0)
	private val p3 = Point2d(0, +1)
	private val p4 = Point2d(+1, +1)
	private val p5 = Point2d(0, +2)
	private val p6 = Point2d(+2, +1)
	private val pInside = Point2d(0.3, 0.3)
	private val pOutside = Point2d(+1, +1)
	private val t1 = Triangle(p1, p2, p3, EdgeContext(), true) // CCW
	private val t2 = Triangle(p3, p4, p2, EdgeContext(), true) // CW
	private val t3 = Triangle(p4, p5, p3, EdgeContext(), true) // CCW
	private val t4 = Triangle(p2, p4, p6, EdgeContext(), true) // CW

	@Test
	fun testArea(): Unit {
		val triangle = Triangle(Point2d(0, 0), Point2d(0, -10), Point2d(+10, 0))
		assertEqualsDouble(50.0, triangle.area, 0.0001)
	}

	@Test
	fun testInstantiated(): Unit {
		assertTrue(t1 is Triangle)
	}

	@Test
	fun testContainsPoint(): Unit {
		assertTrue(t1.containsPoint(p1))
		assertTrue(t1.containsPoint(p2))
		assertTrue(t1.containsPoint(p3))
		assertFalse(t1.containsPoint(pInside))
		assertFalse(t1.containsPoint(pOutside))
	}

	@Test
	fun testContainsEdge(): Unit {
		assertTrue(t1.containsEdge(t1.ctx.createEdge(p1, p2)))
		assertTrue(t1.containsEdge(t1.ctx.createEdge(p2, p3)))
		assertTrue(t1.containsEdge(t1.ctx.createEdge(p3, p1)))
		assertFalse(t1.containsEdge(t1.ctx.createEdge(pInside, pOutside)))
		assertFalse(t1.containsEdge(t1.ctx.createEdge(p1, pOutside)))
		assertFalse(t1.containsEdge(t1.ctx.createEdge(pInside, p3)))
	}

	@Test
	fun testMarkNeighbor(): Unit {
		t1.markNeighborTriangle(t2)
		assertTrue(t1.neighbors[0] == t2 || t1.neighbors[1] == t2 || t1.neighbors[2] == t2)
		assertTrue(t2.neighbors[0] == t1 || t2.neighbors[1] == t1 || t2.neighbors[2] == t1)

		t1.markNeighborTriangle(t3)
		assertTrue(t1.neighbors[0] != t3 && t1.neighbors[1] != t3 && t1.neighbors[2] != t3)
	}

	@Test
	fun testGetPointIndexOffset(): Unit {
		// CCW
		assertEquals(0, t1.getPointIndexOffset(p1))
		assertEquals(1, t1.getPointIndexOffset(p2))
		assertEquals(2, t1.getPointIndexOffset(p3))

		assertEquals(2, t1.getPointIndexOffset(p1, -1))
		assertEquals(0, t1.getPointIndexOffset(p2, -1))
		assertEquals(1, t1.getPointIndexOffset(p3, -1))

		assertEquals(1, t1.getPointIndexOffset(p1, +1))
		assertEquals(2, t1.getPointIndexOffset(p2, +1))
		assertEquals(0, t1.getPointIndexOffset(p3, +1))
	}

	@Test
	fun testPointCW(): Unit {
		assertTrue(t1.pointCW(p1) == p3)
		assertTrue(t1.pointCCW(p1) == p2)
		assertTrue(t1.pointCW(p2) == p1)

		assertTrue(t2.pointCW(p3) == p4)
	}

	@Test
	fun testNeighborCW(): Unit {
		assertEquals(null, t2.neighborCW(p3))

		t2.markNeighborTriangle(t1)
		t2.markNeighborTriangle(t3)
		t2.markNeighborTriangle(t4)

		assertEquals(t3, t2.neighborCW(p3))
		assertEquals(t1, t2.neighborCCW(p3))

		assertEquals(t3, t2.neighborCCW(p4))
		assertEquals(t4, t2.neighborCW(p4))

		assertEquals(t4, t2.neighborCCW(p2))
		assertEquals(t1, t2.neighborCW(p2))
	}

	@Test
	fun testConstrainedEdge(): Unit {
		t1.markConstrainedEdgeByPoints(p1, p3)
		t1.markConstrainedEdgeByPoints(p1, p2)
		assertTrue(t1.getConstrainedEdgeCW(p1))
		assertFalse(t1.getConstrainedEdgeCW(p3))
		assertTrue(t1.getConstrainedEdgeCCW(p1))
		t1.markConstrainedEdgeByIndex(t1.edgeIndex(p3, p2))
		assertTrue(t1.getConstrainedEdgeCW(p3))

		// Constraints not propagated to other triangles.
		assertFalse(t2.getConstrainedEdgeCCW(p3))
	}

	@Test
	fun testSetEdgeSide(): Unit {
		t2.markNeighborTriangle(t1)
		t2.markNeighborTriangle(t3)
		t2.markNeighborTriangle(t4)

		//t2.setEdgeSide(p3, p2);
		t2.isEdgeSide(p3, p2)

		assertTrue(t2.getConstrainedEdgeCCW(p3))
		assertTrue(t1.getConstrainedEdgeCW(p3))
	}

	/*
	public fun testRotateTrianglePair():Unit {
		Triangle.rotateTrianglePair(t1, p3, t2, p2);
	}
	*/
}

class UtilsTest {
	val p1 = Point2d(0, +1)
	val p2 = Point2d(-1, 0)
	val p3 = Point2d(+1, 0)
	val p4 = Point2d(0.0, -0.01)
	val p5 = Point2d(0, -1)

	@Test
	fun testInsideCircleTrue(): Unit {
		assertTrue(Triangle.insideIncircle(p1, p2, p3, p4))
	}

	@Test
	fun testInsideCircleFalse(): Unit {
		assertFalse(Triangle.insideIncircle(p1, p2, p3, p5))
	}
}

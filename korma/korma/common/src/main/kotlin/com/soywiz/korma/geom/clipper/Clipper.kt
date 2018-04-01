/*******************************************************************************
 *                                                                              *
 * Author    :  Angus Johnson                                                   *
 * Version   :  6.2.1                                                           *
 * Website   :  http://www.angusj.com                                           *
 * Copyright :  Angus Johnson 2010-2017                                         *
 *                                                                              *
 * License:                                                                     *
 * Use, modification & distribution is subject to Boost Software License Ver 1. *
 * http://www.boost.org/LICENSE_1_0.txt                                         *
 *                                                                              *
 * Attributions:                                                                *
 * The code in this library is an extension of Bala Vatti's clipping algorithm: *
 * "A generic solution to polygon clipping"                                     *
 * Communications of the ACM, Vol 35, Issue 7 (July 1992) pp 56-63.             *
 * http://portal.acm.org/citation.cfm?id=129906                                 *
 *                                                                              *
 * Computer graphics and geometric modeling: implementation and algorithms      *
 * By Max K. Agoston                                                            *
 * Springer; 1 edition (January 4, 2005)                                        *
 * http://books.google.com/books?q=vatti+clipping+agoston                       *
 *                                                                              *
 * See also:                                                                    *
 * "Polygon Offsetting by Computing Winding Numbers"                            *
 * Paper no. DETC2005-85513 pp. 565-575                                         *
 * ASME 2005 International Design Engineering Technical Conferences             *
 * and Computers and Information in Engineering Conference (IDETC/CIE2005)      *
 * September 24-28, 2005 , Long Beach, California, USA                          *
 * http://www.me.berkeley.edu/~mcmains/pubs/DAC05OffsetPolygon.pdf              *
 *                                                                              *
 *******************************************************************************/

/*******************************************************************************
 *                                                                              *
 * This is a translation of the Delphi Clipper library and the naming style     *
 * used has retained a Delphi flavour.                                          *
 * Delphi -> C# -> Java -> Kotlin                                               *
 *                                                                              *
 *******************************************************************************/

@file:Suppress("CanBeVal")

package com.soywiz.korma.geom.clipper

import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.math.Math
import kotlin.math.*

interface Clipper {
	enum class ClipType { INTERSECTION, UNION, DIFFERENCE, XOR }
	enum class Direction { RIGHT_TO_LEFT, LEFT_TO_RIGHT }
	enum class EndType { CLOSED_POLYGON, CLOSED_LINE, OPEN_BUTT, OPEN_SQUARE, OPEN_ROUND }
	enum class JoinType { SQUARE, ROUND, MITER }
	enum class PolyFillType { EVEN_ODD, NON_ZERO, POSITIVE, NEGATIVE }
	enum class PolyType { SUBJECT, CLIP }

	//interface ZFillCallback {
	//	fun zFill(bot1: Point2d, top1: Point2d, bot2: Point2d, top2: Point2d, pt: Point2d)
	//}

	fun addPath(pg: Path, polyType: PolyType, Closed: Boolean): Boolean
	fun addPaths(ppg: Paths, polyType: PolyType, closed: Boolean): Boolean
	fun clear()
	fun execute(clipType: ClipType, solution: Paths): Boolean
	fun execute(clipType: ClipType, solution: Paths, subjFillType: PolyFillType, clipFillType: PolyFillType): Boolean
	fun execute(clipType: ClipType, polytree: PolyTree): Boolean
	fun execute(clipType: ClipType, polytree: PolyTree, subjFillType: PolyFillType, clipFillType: PolyFillType): Boolean

	companion object {
		//InitOptions that can be passed to the constructor ...
		const val REVERSE_SOLUTION = 1
		const val STRICTLY_SIMPLE = 2
		const val PRESERVE_COLINEAR = 4
	}
}

abstract class ClipperBase protected constructor(val isPreserveCollinear: Boolean) : Clipper { //constructor (nb: no external instantiation)
	protected inner class LocalMinima {
		var y: Double = 0.0
		var leftBound: Edge? = null
		var rightBound: Edge? = null
		var next: LocalMinima? = null
	}

	class Scanbeam {
		var y: Double = 0.0
		var next: Scanbeam? = null
	}

	protected var minimaList: LocalMinima? = null

	protected var currentLM: LocalMinima? = null

	private val edges: MutableList<List<Edge>>

	protected var hasOpenPaths: Boolean = false

	init {
		minimaList = null
		currentLM = null
		hasOpenPaths = false
		edges = ArrayList<List<Edge>>()
	}

	override fun addPath(pg: Path, polyType: Clipper.PolyType, Closed: Boolean): Boolean {
		if (!Closed && polyType == Clipper.PolyType.CLIP) throw IllegalStateException("AddPath: Open paths must be subject.")

		var highI = pg.size - 1
		if (Closed) {
			while (highI > 0 && pg[highI] == pg[0]) --highI
		}
		while (highI > 0 && pg[highI] == pg[highI - 1]) --highI
		if (Closed && highI < 2 || !Closed && highI < 1) return false

		//create a new edge array ...
		val edges = ArrayList<Edge>(highI + 1)
		for (i in 0..highI) {
			edges.add(Edge())
		}

		var IsFlat = true

		//1. Basic (first) edge initialization ...
		edges[1].current = Point2d(pg[1])
		rangeTest(pg[0])
		rangeTest(pg[highI])
		initEdge(edges[0], edges[1], edges[highI], pg[0])
		initEdge(edges[highI], edges[0], edges[highI - 1], pg[highI])
		for (i in highI - 1 downTo 1) {
			rangeTest(pg[i])
			initEdge(edges[i], edges[i + 1], edges[i - 1], pg[i])
		}
		var eStart = edges[0]

		//2. Remove duplicate vertices, and (when closed) collinear edges ...
		var e = eStart
		var eLoopStop = eStart
		while (true) {
			//nb: allows matching start and end points when not Closed ...
			if (e.current == e.next!!.current && (Closed || e.next != eStart)) {
				if (e === e.next) {
					break
				}
				if (e === eStart) {
					eStart = e.next!!
				}
				e = removeEdge(e)
				eLoopStop = e
				continue
			}
			if (e.prev === e.next) {
				break //only two vertices
			} else if (Closed && Points.slopesEqual(e.prev!!.current, e.current, e.next!!.current)
				&& (!isPreserveCollinear || !Points.isPt2BetweenPt1AndPt3(e.prev!!.current, e.current, e.next!!.current))) {
				//Collinear edges are allowed for open paths but in closed paths
				//the default is to merge adjacent collinear edges into a single edge.
				//However, if the PreserveCollinear property is enabled, only overlapping
				//collinear edges (ie spikes) will be removed from closed paths.
				if (e === eStart) {
					eStart = e.next!!
				}
				e = removeEdge(e)
				e = e.prev!!
				eLoopStop = e
				continue
			}
			e = e.next!!
			if (e === eLoopStop || !Closed && e.next === eStart) {
				break
			}
		}

		if (!Closed && e === e.next || Closed && e.prev === e.next) {
			return false
		}

		if (!Closed) {
			hasOpenPaths = true
			eStart.prev!!.outIdx = Edge.SKIP
		}

		//3. Do second stage of edge initialization ...
		e = eStart
		do {
			initEdge2(e, polyType)
			e = e.next!!
			if (IsFlat && e.current.y != eStart.current.y) {
				IsFlat = false
			}
		} while (e !== eStart)

		//4. Finally, add edge bounds to LocalMinima list ...

		//Totally flat paths must be handled differently when adding them
		//to LocalMinima list to avoid endless loops etc ...
		if (IsFlat) {
			if (Closed) {
				return false
			}
			e.prev!!.outIdx = Edge.SKIP
			if (e.prev!!.bot.x < e.prev!!.top.x) {
				e.prev!!.reverseHorizontal()
			}
			val locMin = LocalMinima()
			locMin.next = null
			locMin.y = e.bot.y
			locMin.leftBound = null
			locMin.rightBound = e
			locMin.rightBound!!.side = Edge.Side.RIGHT
			locMin.rightBound!!.windDelta = 0
			while (e.next!!.outIdx != Edge.SKIP) {
				e.nextInLML = e.next
				if (e.bot.x != e.prev!!.top.x) {
					e.reverseHorizontal()
				}
				e = e.next!!
			}
			insertLocalMinima(locMin)
			this.edges.add(edges)
			return true
		}

		this.edges.add(edges)
		var leftBoundIsForward: Boolean
		var EMin: Edge? = null

		//workaround to avoid an endless loop in the while loop below when
		//open paths have matching start and end points ...
		if (e.prev!!.bot == e.prev!!.top) {
			e = e.next!!
		}

		while (true) {
			e = e.findNextLocMin()
			if (e === EMin) break
			if (EMin == null) EMin = e

			//E and E.Prev now share a local minima (left aligned if horizontal).
			//Compare their slopes to find which starts which bound ...
			val locMin = LocalMinima()
			locMin.next = null
			locMin.y = e.bot.y
			if (e.deltaX < e.prev!!.deltaX) {
				locMin.leftBound = e.prev
				locMin.rightBound = e
				leftBoundIsForward = false //Q.nextInLML = Q.prev
			} else {
				locMin.leftBound = e
				locMin.rightBound = e.prev
				leftBoundIsForward = true //Q.nextInLML = Q.next
			}
			locMin.leftBound!!.side = Edge.Side.LEFT
			locMin.rightBound!!.side = Edge.Side.RIGHT

			locMin.leftBound!!.windDelta = when {
				!Closed -> 0
				locMin.leftBound!!.next === locMin.rightBound -> -1
				else -> 1
			}

			locMin.rightBound!!.windDelta = -locMin.leftBound!!.windDelta

			e = processBound(locMin.leftBound!!, leftBoundIsForward)
			if (e.outIdx == Edge.SKIP) e = processBound(e, leftBoundIsForward)

			var E2 = processBound(locMin.rightBound!!, !leftBoundIsForward)
			if (E2.outIdx == Edge.SKIP) E2 = processBound(E2, !leftBoundIsForward)

			if (locMin.leftBound!!.outIdx == Edge.SKIP) {
				locMin.leftBound = null
			} else if (locMin.rightBound!!.outIdx == Edge.SKIP) {
				locMin.rightBound = null
			}
			insertLocalMinima(locMin)
			if (!leftBoundIsForward) e = E2
		}
		return true

	}

	override fun addPaths(ppg: Paths, polyType: Clipper.PolyType, closed: Boolean): Boolean {
		var result = false
		for (i in ppg.indices) {
			if (addPath(ppg[i], polyType, closed)) result = true
		}
		return result
	}

	override fun clear() {
		disposeLocalMinimaList()
		edges.clear()
		hasOpenPaths = false
	}

	private fun disposeLocalMinimaList() {
		while (minimaList != null) {
			val tmpLm = minimaList!!.next
			minimaList = null
			minimaList = tmpLm
		}
		currentLM = null
	}

	private fun insertLocalMinima(newLm: LocalMinima) {
		if (minimaList == null) {
			minimaList = newLm
		} else if (newLm.y >= minimaList!!.y) {
			newLm.next = minimaList
			minimaList = newLm
		} else {
			var tmpLm: LocalMinima = minimaList!!
			while (tmpLm.next != null && newLm.y < tmpLm.next!!.y) {
				tmpLm = tmpLm.next!!
			}
			newLm.next = tmpLm.next
			tmpLm.next = newLm
		}
	}

	protected fun popLocalMinima() {
		if (currentLM == null) return
		currentLM = currentLM!!.next
	}

	private fun processBound(ee: Edge, LeftBoundIsForward: Boolean): Edge {
		var e = ee
		var EStart: Edge
		var result = e
		var Horz: Edge

		if (result.outIdx == Edge.SKIP) {
			//check if there are edges beyond the skip edge in the bound and if so
			//create another LocMin and calling ProcessBound once more ...
			e = result
			if (LeftBoundIsForward) {
				while (e.top.y == e.next!!.bot.y) e = e.next!!
				while (e !== result && e.deltaX == Edge.HORIZONTAL) e = e.prev!!
			} else {
				while (e.top.y == e.prev!!.bot.y) e = e.prev!!
				while (e !== result && e.deltaX == Edge.HORIZONTAL) e = e.next!!
			}
			if (e === result) {
				result = if (LeftBoundIsForward) e.next!! else e.prev!!
			} else {
				//there are more edges in the bound beyond result starting with E
				e = if (LeftBoundIsForward) result.next!! else result.prev!!
				val locMin = LocalMinima()
				locMin.next = null
				locMin.y = e.bot.y
				locMin.leftBound = null
				locMin.rightBound = e
				e.windDelta = 0
				result = processBound(e, LeftBoundIsForward)
				insertLocalMinima(locMin)
			}
			return result
		}

		if (e.deltaX == Edge.HORIZONTAL) {
			//We need to be careful with open paths because this may not be a
			//true local minima (ie E may be following a skip edge).
			//Also, consecutive horz. edges may start heading left before going right.
			EStart = if (LeftBoundIsForward) e.prev!! else e.next!!
			if (EStart.outIdx != Edge.SKIP) {
				if (EStart.deltaX == Edge.HORIZONTAL)
				//ie an adjoining horizontal skip edge
				{
					if (EStart.bot.x != e.bot.x && EStart.top.x != e.bot.x) {
						e.reverseHorizontal()
					}
				} else if (EStart.bot.x != e.bot.x) {
					e.reverseHorizontal()
				}
			}
		}

		EStart = e
		if (LeftBoundIsForward) {
			while (result.top.y == result.next!!.bot.y && result.next!!.outIdx != Edge.SKIP) {
				result = result.next!!
			}
			if (result.deltaX == Edge.HORIZONTAL && result.next!!.outIdx != Edge.SKIP) {
				//nb: at the top of a bound, horizontals are added to the bound
				//only when the preceding edge attaches to the horizontal's left vertex
				//unless a Skip edge is encountered when that becomes the top divide
				Horz = result
				while (Horz.prev!!.deltaX == Edge.HORIZONTAL) {
					Horz = Horz.prev!!
				}
				if (Horz.prev!!.top.x == result.next!!.top.x) {
					if (!LeftBoundIsForward) {
						result = Horz.prev!!
					}
				} else if (Horz.prev!!.top.x > result.next!!.top.x) {
					result = Horz.prev!!
				}
			}
			while (e !== result) {
				e.nextInLML = e.next
				if (e.deltaX == Edge.HORIZONTAL && e !== EStart && e.bot.x != e.prev!!.top.x) {
					e.reverseHorizontal()
				}
				e = e.next!!
			}
			if (e.deltaX == Edge.HORIZONTAL && e !== EStart && e.bot.x != e.prev!!.top.x) {
				e.reverseHorizontal()
			}
			result = result.next!! //move to the edge just beyond current bound
		} else {
			while (result.top.y == result.prev!!.bot.y && result.prev!!.outIdx != Edge.SKIP) result = result.prev!!
			if (result.deltaX == Edge.HORIZONTAL && result.prev!!.outIdx != Edge.SKIP) {
				Horz = result
				while (Horz.next!!.deltaX == Edge.HORIZONTAL) Horz = Horz.next!!
				if (Horz.next!!.top.x == result.prev!!.top.x) {
					if (!LeftBoundIsForward) result = Horz.next!!
				} else if (Horz.next!!.top.x > result.prev!!.top.x) result = Horz.next!!
			}

			while (e !== result) {
				e.nextInLML = e.prev
				if (e.deltaX == Edge.HORIZONTAL && e !== EStart && e.bot.x != e.next!!.top.x) {
					e.reverseHorizontal()
				}
				e = e.prev!!
			}
			if (e.deltaX == Edge.HORIZONTAL && e !== EStart && e.bot.x != e.next!!.top.x) {
				e.reverseHorizontal()
			}
			result = result.prev!! //move to the edge just beyond current bound
		}
		return result
	}

	protected open fun reset() {
		currentLM = minimaList
		if (currentLM == null) {
			return  //ie nothing to process
		}

		//reset all edges ...
		var lm = minimaList
		while (lm != null) {
			var e = lm.leftBound
			if (e != null) {
				e.current = Point2d(e.bot)
				e.side = Edge.Side.LEFT
				e.outIdx = Edge.UNASSIGNED
			}
			e = lm.rightBound
			if (e != null) {
				e.current = Point2d(e.bot)
				e.side = Edge.Side.RIGHT
				e.outIdx = Edge.UNASSIGNED
			}
			lm = lm.next
		}
	}

	companion object {

		private fun initEdge(e: Edge, eNext: Edge, ePrev: Edge, pt: Point2d) {
			e.next = eNext
			e.prev = ePrev
			e.current = Point2d(pt)
			e.outIdx = Edge.UNASSIGNED
		}

		private fun initEdge2(e: Edge, polyType: Clipper.PolyType) {
			if (e.current.y >= e.next!!.current.y) {
				e.bot = Point2d(e.current)
				e.top = Point2d(e.next!!.current)
			} else {
				e.top = Point2d(e.current)
				e.bot = Point2d(e.next!!.current)
			}
			e.updateDeltaX()
			e.polyTyp = polyType
		}

		private fun rangeTest(Pt: Point2d) {
			if (Pt.x > LOW_RANGE || Pt.y > LOW_RANGE || -Pt.x > LOW_RANGE || -Pt.y > LOW_RANGE) {
				if (Pt.x > HI_RANGE || Pt.y > HI_RANGE || -Pt.x > HI_RANGE || -Pt.y > HI_RANGE) {
					throw IllegalStateException("Coordinate outside allowed range")
				}
			}
		}

		private fun removeEdge(e: Edge): Edge {
			//removes e from double_linked_list (but without removing from memory)
			e.prev!!.next = e.next
			e.next!!.prev = e.prev
			val result = e.next
			e.prev = null //flag as removed (see ClipperBase.Clear)
			return result!!
		}

		private val LOW_RANGE: Long = 0x3FFFFFFF

		private val HI_RANGE = 0x3FFFFFFFFFFFFFFFL
	}

}

//class ClipperOffset @JvmOverloads constructor(private val miterLimit: Double = 2.0, private val arcTolerance: Double = ClipperOffset.DEFAULT_ARC_TOLERANCE) {
class ClipperOffset constructor(private val miterLimit: Double = 2.0, private val arcTolerance: Double = ClipperOffset.DEFAULT_ARC_TOLERANCE) {

	private var destPolys: Paths? = null
	private var srcPoly: Path? = null
	private var destPoly: Path? = null

	private val normals: MutableList<Point2d>
	private var delta: Double = 0.0
	private var inA: Double = 0.0
	private var sin: Double = 0.0
	private var cos: Double = 0.0

	private var miterLim: Double = 0.0
	private var stepsPerRad: Double = 0.0
	private var lowest: Point2d? = null

	private val polyNodes: PolyNode

	init {
		lowest = Point2d()
		lowest!!.x = -1.toDouble()
		polyNodes = PolyNode()
		normals = ArrayList<Point2d>()
	}

	fun addPath(path: Path, joinType: Clipper.JoinType, endType: Clipper.EndType) {
		var highI = path.size - 1
		if (highI < 0) {
			return
		}
		val newNode = PolyNode()
		newNode.joinType = joinType
		newNode.endType = endType

		//strip duplicate points from path and also get index to the lowest point ...
		if (endType == Clipper.EndType.CLOSED_LINE || endType == Clipper.EndType.CLOSED_POLYGON) {
			while (highI > 0 && path[0] === path[highI]) {
				highI--
			}
		}

		newNode.polygon.add(path[0])
		var j = 0
		var k = 0
		for (i in 1..highI) {
			if (newNode.polygon[j] !== path[i]) {
				j++
				newNode.polygon.add(path[i])
				if (path[i].y > newNode.polygon[k].y || path[i].y == newNode.polygon[k].y && path[i].x < newNode.polygon[k].x) {
					k = j
				}
			}
		}
		if (endType == Clipper.EndType.CLOSED_POLYGON && j < 2) {
			return
		}

		polyNodes.addChild(newNode)

		//if this path's lowest pt is lower than all the others then update m_lowest
		if (endType != Clipper.EndType.CLOSED_POLYGON) {
			return
		}
		if (lowest!!.x < 0) {
			lowest = Point2d((polyNodes.childCount - 1), k)
		} else {
			val ip = polyNodes.getChilds()[lowest!!.x.toInt()].polygon[lowest!!.y.toInt()]
			if (newNode.polygon[k].y > ip.y || newNode.polygon[k].y == ip.y && newNode.polygon[k].x < ip.x) {
				lowest = Point2d((polyNodes.childCount - 1), k)
			}
		}
	}

	fun addPaths(paths: Paths, joinType: Clipper.JoinType, endType: Clipper.EndType) {
		for (p in paths) {
			addPath(p, joinType, endType)
		}
	}

	fun clear() {
		polyNodes._childs.clear()
		lowest!!.x = -1.toDouble()
	}

	private fun doMiter(j: Int, k: Int, r: Double) {
		val q = delta / r
		destPoly!!.add(
			Point2d(
				round(srcPoly!![j].x + (normals[k].x + normals[j].x) * q).toInt(),
				round(srcPoly!![j].y + (normals[k].y + normals[j].y) * q).toInt()
			)
		)
	}

	private fun doOffset(delta: Double) {
		destPolys = Paths()
		this.delta = delta

		//if Zero offset, just copy any CLOSED polygons to m_p and return ...
		if (nearZero(delta)) {
			for (i in 0..polyNodes.childCount - 1) {
				val node = polyNodes.getChilds()[i]
				if (node.endType == Clipper.EndType.CLOSED_POLYGON) {
					destPolys!!.add(node.polygon)
				}
			}
			return
		}

		//see offset_triginometry3.svg in the documentation folder ...
		if (miterLimit > 2) {
			miterLim = 2 / (miterLimit * miterLimit)
		} else {
			miterLim = 0.5
		}

		val y: Double
		if (arcTolerance <= 0.0) {
			y = DEFAULT_ARC_TOLERANCE
		} else if (arcTolerance > abs(delta) * DEFAULT_ARC_TOLERANCE) {
			y = abs(delta) * DEFAULT_ARC_TOLERANCE
		} else {
			y = arcTolerance
		}
		//see offset_triginometry2.svg in the documentation folder ...
		val steps = PI / acos(1 - y / abs(delta))
		sin = kotlin.math.sin(TWO_PI / steps)
		cos = kotlin.math.cos(TWO_PI / steps)
		stepsPerRad = steps / TWO_PI
		if (delta < 0.0) {
			sin = -sin
		}

		for (i in 0..polyNodes.childCount - 1) {
			val node = polyNodes.getChilds()[i]
			srcPoly = node.polygon

			val len = srcPoly!!.size

			if (len == 0 || delta <= 0 && (len < 3 || node.endType != Clipper.EndType.CLOSED_POLYGON)) {
				continue
			}

			destPoly = Path()

			if (len == 1) {
				if (node.joinType == Clipper.JoinType.ROUND) {
					var X = 1.0
					var Y = 0.0
					var j = 1
					while (j <= steps) {
						destPoly!!.add(Point2d(round(srcPoly!![0].x + X * delta).toInt(), round(srcPoly!![0].y + Y * delta).toInt()))
						val X2 = X
						X = X * cos - sin * Y
						Y = X2 * sin + Y * cos
						j++
					}
				} else {
					var X = -1.0
					var Y = -1.0
					for (j in 0..3) {
						destPoly!!.add(Point2d(round(srcPoly!![0].x + X * delta).toInt(), round(srcPoly!![0].y + Y * delta).toInt()))
						if (X < 0) {
							X = 1.0
						} else if (Y < 0) {
							Y = 1.0
						} else {
							X = -1.0
						}
					}
				}
				destPolys!!.add(destPoly!!)
				continue
			}

			//build m_normals ...
			normals.clear()
			for (j in 0..len - 1 - 1) {
				normals.add(Points.getUnitNormal(srcPoly!![j], srcPoly!![j + 1]))
			}
			if (node.endType == Clipper.EndType.CLOSED_LINE || node.endType == Clipper.EndType.CLOSED_POLYGON) {
				normals.add(Points.getUnitNormal(srcPoly!![len - 1], srcPoly!![0]))
			} else {
				normals.add(Point2d(normals[len - 2]))
			}

			if (node.endType == Clipper.EndType.CLOSED_POLYGON) {
				val k = intArrayOf(len - 1)
				for (j in 0..len - 1) {
					offsetPoint(j, k, node.joinType!!)
				}
				destPolys!!.add(destPoly!!)
			} else if (node.endType == Clipper.EndType.CLOSED_LINE) {
				val k = intArrayOf(len - 1)
				for (j in 0..len - 1) {
					offsetPoint(j, k, node.joinType!!)
				}
				destPolys!!.add(destPoly!!)
				destPoly = Path()
				//re-build m_normals ...
				val n = normals[len - 1]
				for (j in len - 1 downTo 1) {
					normals[j] = Point2d(-normals[j - 1].x, -normals[j - 1].y)
				}
				normals[0] = Point2d(-n.x, -n.y)
				k[0] = 0
				for (j in len - 1 downTo 0) {
					offsetPoint(j, k, node.joinType!!)
				}
				destPolys!!.add(destPoly!!)
			} else {
				val k = IntArray(1)
				for (j in 1..len - 1 - 1) {
					offsetPoint(j, k, node.joinType!!)
				}

				var pt1: Point2d
				if (node.endType == Clipper.EndType.OPEN_BUTT) {
					val j = len - 1
					pt1 = Point2d(
						round(srcPoly!![j].x + normals[j].x * delta).toInt(),
						round(srcPoly!![j].y + normals[j].y * delta).toInt()
					)
					destPoly!!.add(pt1)
					pt1 = Point2d(
						round(srcPoly!![j].x - normals[j].x * delta).toInt(),
						round(srcPoly!![j].y - normals[j].y * delta).toInt()
					)
					destPoly!!.add(pt1)
				} else {
					val j = len - 1
					k[0] = len - 2
					inA = 0.0
					normals[j] = Point2d(-normals[j].x, -normals[j].y)
					if (node.endType == Clipper.EndType.OPEN_SQUARE) {
						doSquare(j, k[0])
					} else {
						doRound(j, k[0])
					}
				}

				//re-build m_normals ...
				for (j in len - 1 downTo 1) {
					normals[j] = Point2d(-normals[j - 1].x, -normals[j - 1].y)
				}

				normals[0] = Point2d(-normals[1].x, -normals[1].y)

				k[0] = len - 1
				for (j in k[0] - 1 downTo 1) offsetPoint(j, k, node.joinType!!)

				if (node.endType == Clipper.EndType.OPEN_BUTT) {
					pt1 = Point2d(round(srcPoly!![0].x - normals[0].x * delta).toInt(), round(srcPoly!![0].y - normals[0].y * delta).toInt())
					destPoly!!.add(pt1)
					pt1 = Point2d(round(srcPoly!![0].x + normals[0].x * delta).toInt(), round(srcPoly!![0].y + normals[0].y * delta).toInt())
					destPoly!!.add(pt1)
				} else {
					k[0] = 1
					inA = 0.0
					if (node.endType == Clipper.EndType.OPEN_SQUARE) {
						doSquare(0, 1)
					} else {
						doRound(0, 1)
					}
				}
				destPolys!!.add(destPoly!!)
			}
		}
	}

	private fun doRound(j: Int, k: Int) {
		val a = atan2(inA, normals[k].x * normals[j].x + normals[k].y * normals[j].y)
		val steps = Math.max(round(stepsPerRad * abs(a)).toInt(), 1)

		var X = normals[k].x
		var Y = normals[k].y
		var X2: Double
		for (i in 0 until steps) {
			destPoly!!.add(Point2d(round(srcPoly!![j].x + X * delta).toInt(), round(srcPoly!![j].y + Y * delta).toInt()))
			X2 = X
			X = X * cos - sin * Y
			Y = X2 * sin + Y * cos
		}
		destPoly!!.add(Point2d(round(srcPoly!![j].x + normals[j].x * delta).toInt(), round(srcPoly!![j].y + normals[j].y * delta).toInt()))
	}

	private fun doSquare(j: Int, k: Int) {
		val nkx = normals[k].x
		val nky = normals[k].y
		val njx = normals[j].x
		val njy = normals[j].y
		val sjx = srcPoly!![j].x
		val sjy = srcPoly!![j].y
		val dx = tan(atan2(inA, nkx * njx + nky * njy) / 4)
		destPoly!!.add(Point2d(round(sjx + delta * (nkx - nky * dx)).toInt(), round(sjy + delta * (nky + nkx * dx)).toInt()))
		destPoly!!.add(Point2d(round(sjx + delta * (njx + njy * dx)).toInt(), round(sjy + delta * (njy - njx * dx)).toInt()))
	}

	//------------------------------------------------------------------------------

	fun execute(solution: Paths, delta: Double) {
		solution.clear()
		fixOrientations()
		doOffset(delta)
		//now clean up 'corners' ...
		val clpr = DefaultClipper(Clipper.REVERSE_SOLUTION)
		clpr.addPaths(destPolys!!, Clipper.PolyType.SUBJECT, true)
		if (delta > 0) {
			clpr.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.POSITIVE, Clipper.PolyFillType.POSITIVE)
		} else {
			val r = destPolys!!.bounds
			val outer = Path(4)

			outer.add(Point2d(r.left - 10, r.bottom + 10))
			outer.add(Point2d(r.right + 10, r.bottom + 10))
			outer.add(Point2d(r.right + 10, r.top - 10))
			outer.add(Point2d(r.left - 10, r.top - 10))

			clpr.addPath(outer, Clipper.PolyType.SUBJECT, true)

			clpr.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.NEGATIVE, Clipper.PolyFillType.NEGATIVE)
			if (solution.size > 0) {
				solution.removeAt(0)
			}
		}
	}

	//------------------------------------------------------------------------------

	fun execute(solution: PolyTree, delta: Double) {
		solution.Clear()
		fixOrientations()
		doOffset(delta)

		//now clean up 'corners' ...
		val clpr = DefaultClipper(Clipper.REVERSE_SOLUTION)
		clpr.addPaths(destPolys!!, Clipper.PolyType.SUBJECT, true)
		if (delta > 0) {
			clpr.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.POSITIVE, Clipper.PolyFillType.POSITIVE)
		} else {
			val r = destPolys!!.bounds
			val outer = Path(4)

			outer.add(Point2d(r.left - 10, r.bottom + 10))
			outer.add(Point2d(r.right + 10, r.bottom + 10))
			outer.add(Point2d(r.right + 10, r.top - 10))
			outer.add(Point2d(r.left - 10, r.top - 10))

			clpr.addPath(outer, Clipper.PolyType.SUBJECT, true)

			clpr.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.NEGATIVE, Clipper.PolyFillType.NEGATIVE)
			//remove the outer PolyNode rectangle ...
			if (solution.childCount == 1 && solution.getChilds()[0].childCount > 0) {
				val outerNode = solution.getChilds()[0]
				solution._childs[0] = outerNode.getChilds()[0]
				solution._childs[0].parent = solution
				for (i in 1 until outerNode.childCount) {
					solution.addChild(outerNode.getChilds()[i])
				}
			} else {
				solution.Clear()
			}
		}
	}

	//------------------------------------------------------------------------------

	private fun fixOrientations() {
		//fixup orientations of all closed paths if the orientation of the
		//closed path with the lowermost vertex is wrong ...
		if (lowest!!.x >= 0 && !polyNodes._childs[lowest!!.x.toInt()].polygon.orientation()) {
			for (i in 0 until polyNodes.childCount) {
				val node = polyNodes._childs[i]
				if (node.endType == Clipper.EndType.CLOSED_POLYGON || node.endType == Clipper.EndType.CLOSED_LINE && node.polygon.orientation()) {
					node.polygon.reverse()
				}
			}
		} else {
			for (i in 0 until polyNodes.childCount) {
				val node = polyNodes._childs[i]
				if (node.endType == Clipper.EndType.CLOSED_LINE && !node.polygon.orientation()) {
					node.polygon.reverse()
				}
			}
		}
	}

	private fun offsetPoint(j: Int, kV: IntArray, jointype: Clipper.JoinType) {
		//cross product ...
		val k = kV[0]
		val nkx = normals[k].x
		val nky = normals[k].y
		val njy = normals[j].y
		val njx = normals[j].x
		val sjx = srcPoly!![j].x
		val sjy = srcPoly!![j].y
		inA = nkx * njy - njx * nky

		if (abs(inA * delta) < 1.0) {
			//dot product ...

			val cosA = nkx * njx + njy * nky
			if (cosA > 0)
			// angle ==> 0 degrees
			{
				destPoly!!.add(Point2d(round(sjx + nkx * delta).toInt(), round(sjy + nky * delta).toInt()))
				return
			}
			//else angle ==> 180 degrees
		} else if (inA > 1.0) {
			inA = 1.0
		} else if (inA < -1.0) {
			inA = -1.0
		}

		if (inA * delta < 0) {
			destPoly!!.add(Point2d(round(sjx + nkx * delta).toInt(), round(sjy + nky * delta).toInt()))
			destPoly!!.add(srcPoly!![j])
			destPoly!!.add(Point2d(round(sjx + njx * delta).toInt(), round(sjy + njy * delta).toInt()))
		} else {
			when (jointype) {
				Clipper.JoinType.MITER -> {
					val r = 1.0 + njx * nkx + njy * nky
					if (r >= miterLim) {
						doMiter(j, k, r)
					} else {
						doSquare(j, k)
					}
				}
				Clipper.JoinType.SQUARE -> doSquare(j, k)
				Clipper.JoinType.ROUND -> doRound(j, k)
			}
		}
		kV[0] = j
	}

	companion object {
		private fun nearZero(`val`: Double): Boolean {
			return `val` > -TOLERANCE && `val` < TOLERANCE
		}

		private val TWO_PI = PI * 2

		private val DEFAULT_ARC_TOLERANCE = 0.25

		private val TOLERANCE = 1.0E-20
	}
	//------------------------------------------------------------------------------
}

@Suppress("unused")
//class DefaultClipper @JvmOverloads constructor(InitOptions: Int = 0) //constructor
class DefaultClipper constructor(InitOptions: Int = 0) //constructor
	: ClipperBase(Clipper.PRESERVE_COLINEAR and InitOptions != 0) {
	private inner class IntersectNode {
		var edge1: Edge? = null
		var Edge2: Edge? = null
		var pt: Point2d? = null

	}

	private val polyOuts: MutableList<Path.OutRec>

	private var clipType: Clipper.ClipType? = null

	private var scanbeam: Scanbeam? = null

	private var activeEdges: Edge? = null

	private var sortedEdges: Edge? = null

	private val intersectList: MutableList<IntersectNode>

	private val intersectNodeComparer: Comparator<IntersectNode>

	private var clipFillType: Clipper.PolyFillType? = null

	//------------------------------------------------------------------------------

	private var subjFillType: Clipper.PolyFillType? = null

	//------------------------------------------------------------------------------

	private val joins: MutableList<Path.Join>

	//------------------------------------------------------------------------------

	private val ghostJoins: MutableList<Path.Join>

	private var usingPolyTree: Boolean = false

	//var zFillFunction: Clipper.ZFillCallback? = null

	//------------------------------------------------------------------------------

	private val reverseSolution: Boolean

	//------------------------------------------------------------------------------

	private val strictlySimple: Boolean

	init {
		scanbeam = null
		activeEdges = null
		sortedEdges = null
		intersectList = ArrayList<IntersectNode>()
		intersectNodeComparer = Comparator({ node1, node2 ->
			val i = node2.pt!!.y - node1.pt!!.y
			if (i > 0) {
				1
			} else if (i < 0) {
				-1
			} else {
				0
			}
		})

		usingPolyTree = false
		polyOuts = ArrayList<Path.OutRec>()
		joins = ArrayList<Path.Join>()
		ghostJoins = ArrayList<Path.Join>()
		reverseSolution = Clipper.REVERSE_SOLUTION and InitOptions != 0
		strictlySimple = Clipper.STRICTLY_SIMPLE and InitOptions != 0

		//zFillFunction = null
	}

	private fun addEdgeToSEL(edge: Edge) {
		//SEL pointers in PEdge are reused to build a list of horizontal edges.
		//However, we don't need to worry about order with horizontal edge processing.
		if (sortedEdges == null) {
			sortedEdges = edge
			edge.prevInSEL = null
			edge.nextInSEL = null
		} else {
			edge.nextInSEL = sortedEdges
			edge.prevInSEL = null
			sortedEdges!!.prevInSEL = edge
			sortedEdges = edge
		}
	}

	private fun addGhostJoin(Op: Path.OutPt, OffPt: Point2d) {
		val j = Path.Join()
		j.outPt1 = Op
		j.offPt = Point2d(OffPt)
		ghostJoins.add(j)
	}

	//------------------------------------------------------------------------------

	private fun addJoin(Op1: Path.OutPt, Op2: Path.OutPt, OffPt: Point2d) {
		val j = Path.Join()
		j.outPt1 = Op1
		j.outPt2 = Op2
		j.offPt = Point2d(OffPt)
		joins.add(j)
	}

	//------------------------------------------------------------------------------

	private fun addLocalMaxPoly(e1: Edge, e2: Edge, pt: Point2d) {
		addOutPt(e1, pt)
		if (e2.windDelta == 0) {
			addOutPt(e2, pt)
		}
		if (e1.outIdx == e2.outIdx) {
			e1.outIdx = Edge.UNASSIGNED
			e2.outIdx = Edge.UNASSIGNED
		} else if (e1.outIdx < e2.outIdx) {
			appendPolygon(e1, e2)
		} else {
			appendPolygon(e2, e1)
		}
	}

	//------------------------------------------------------------------------------

	private fun addLocalMinPoly(e1: Edge, e2: Edge, pt: Point2d): Path.OutPt {
		val result: Path.OutPt
		val e: Edge
		val prevE: Edge?
		if (e2.isHorizontal || e1.deltaX > e2.deltaX) {
			result = addOutPt(e1, pt)
			e2.outIdx = e1.outIdx
			e1.side = Edge.Side.LEFT
			e2.side = Edge.Side.RIGHT
			e = e1
			prevE = if (e.prevInAEL === e2) e2.prevInAEL else e.prevInAEL
		} else {
			result = addOutPt(e2, pt)
			e1.outIdx = e2.outIdx
			e1.side = Edge.Side.RIGHT
			e2.side = Edge.Side.LEFT
			e = e2
			prevE = if (e.prevInAEL === e1) e1.prevInAEL else e.prevInAEL
		}

		if (prevE != null && prevE.outIdx >= 0 && Edge.topX(prevE, pt.y) == Edge.topX(e, pt.y) && Edge.slopesEqual(e, prevE) && e.windDelta != 0 && prevE.windDelta != 0) {
			val outPt = addOutPt(prevE, pt)
			addJoin(result, outPt, e.top)
		}
		return result
	}

	private fun addOutPt(e: Edge, pt: Point2d): Path.OutPt {
		val ToFront = e.side == Edge.Side.LEFT
		if (e.outIdx < 0) {
			val outRec = createOutRec()
			outRec.isOpen = e.windDelta == 0
			val newOp = Path.OutPt()
			outRec.points = newOp
			newOp.idx = outRec.Idx
			newOp.pt = Point2d(pt)
			newOp.next = newOp
			newOp.prev = newOp
			if (!outRec.isOpen) setHoleState(e, outRec)
			e.outIdx = outRec.Idx //nb: do this after SetZ !
			return newOp
		} else {

			val outRec = polyOuts[e.outIdx]
			//OutRec.Pts is the 'Left-most' point & OutRec.Pts.Prev is the 'Right-most'
			val op = outRec.points!!
			if (ToFront && pt == op.pt) {
				return op
			} else if (!ToFront && pt == op.prev!!.pt) {
				return op.prev!!
			}

			val newOp = Path.OutPt()
			newOp.idx = outRec.Idx
			newOp.pt = Point2d(pt)
			newOp.next = op
			newOp.prev = op.prev
			newOp.prev!!.next = newOp
			op.prev = newOp
			if (ToFront) outRec.points = newOp
			return newOp
		}
	}

	private fun appendPolygon(e1: Edge, e2: Edge) {
		//get the start and ends of both output polygons ...
		val outRec1 = polyOuts[e1.outIdx]
		val outRec2 = polyOuts[e2.outIdx]

		val holeStateRec: Path.OutRec
		holeStateRec = if (isParam1RightOfParam2(outRec1, outRec2)) {
			outRec2
		} else if (isParam1RightOfParam2(outRec2, outRec1)) {
			outRec1
		} else {
			Path.OutPt.getLowerMostRec(outRec1, outRec2)
		}

		val p1_lft = outRec1.points
		val p1_rt = p1_lft!!.prev!!
		val p2_lft = outRec2.points
		val p2_rt = p2_lft!!.prev!!

		val side: Edge.Side
		//join e2 poly onto e1 poly and delete pointers to e2 ...
		if (e1.side == Edge.Side.LEFT) {
			if (e2.side == Edge.Side.LEFT) {
				//z y x a b c
				p2_lft.reversePolyPtLinks()
				p2_lft.next = p1_lft
				p1_lft.prev = p2_lft
				p1_rt.next = p2_rt
				p2_rt.prev = p1_rt
				outRec1.points = p2_rt
			} else {
				//x y z a b c
				p2_rt.next = p1_lft
				p1_lft.prev = p2_rt
				p2_lft.prev = p1_rt
				p1_rt.next = p2_lft
				outRec1.points = p2_lft
			}
			side = Edge.Side.LEFT
		} else {
			if (e2.side == Edge.Side.RIGHT) {
				//a b c z y x
				p2_lft.reversePolyPtLinks()
				p1_rt.next = p2_rt
				p2_rt.prev = p1_rt
				p2_lft.next = p1_lft
				p1_lft.prev = p2_lft
			} else {
				//a b c x y z
				p1_rt.next = p2_lft
				p2_lft.prev = p1_rt
				p1_lft.prev = p2_rt
				p2_rt.next = p1_lft
			}
			side = Edge.Side.RIGHT
		}
		outRec1.bottomPt = null
		if (holeStateRec == outRec2) {
			if (outRec2.firstLeft !== outRec1) outRec1.firstLeft = outRec2.firstLeft
			outRec1.isHole = outRec2.isHole
		}
		outRec2.points = null
		outRec2.bottomPt = null

		outRec2.firstLeft = outRec1

		val OKIdx = e1.outIdx
		val ObsoleteIdx = e2.outIdx

		e1.outIdx = Edge.UNASSIGNED //nb: safe because we only get here via AddLocalMaxPoly
		e2.outIdx = Edge.UNASSIGNED

		var e = activeEdges
		while (e != null) {
			if (e.outIdx == ObsoleteIdx) {
				e.outIdx = OKIdx
				e.side = side
				break
			}
			e = e.nextInAEL
		}
		outRec2.Idx = outRec1.Idx
	}

	//------------------------------------------------------------------------------

	private fun buildIntersectList(topY: Double) {
		if (activeEdges == null) return

		//prepare for sorting ...
		var e = activeEdges
		sortedEdges = e
		while (e != null) {
			e.prevInSEL = e.prevInAEL
			e.nextInSEL = e.nextInAEL
			e.current.x = Edge.topX(e, topY)
			e = e.nextInAEL
		}

		//bubblesort ...
		var isModified = true
		while (isModified && sortedEdges != null) {
			isModified = false
			e = sortedEdges
			while (e!!.nextInSEL != null) {
				val eNext = e.nextInSEL
				val pt = Array(1) { Point2d() }
				if (e.current.x > eNext!!.current.x) {
					intersectPoint(e, eNext, pt)
					val newNode = IntersectNode()
					newNode.edge1 = e
					newNode.Edge2 = eNext
					newNode.pt = Point2d(pt[0])
					intersectList.add(newNode)

					swapPositionsInSEL(e, eNext)
					isModified = true
				} else {
					e = eNext
				}
			}
			if (e.prevInSEL != null) {
				e.prevInSEL!!.nextInSEL = null
			} else {
				break
			}
		}
		sortedEdges = null
	}

	//------------------------------------------------------------------------------

	private fun buildResult(polyg: Paths) {
		polyg.clear()
		for (i in polyOuts.indices) {
			val outRec = polyOuts[i]
			if (outRec.points == null) continue
			var p: Path.OutPt = outRec.points!!.prev!!
			val cnt = p.pointCount
			if (cnt < 2) continue
			val pg = Path(cnt)
			for (j in 0..cnt - 1) {
				pg.add(Point2d(p.pt))
				p = p.prev!!
			}
			polyg.add(pg)
		}
	}

	private fun buildResult2(polytree: PolyTree) {
		polytree.Clear()

		//add each output polygon/contour to polytree ...
		for (i in polyOuts.indices) {
			val outRec = polyOuts[i]
			val cnt = outRec.points!!.pointCount
			if (outRec.isOpen && cnt < 2 || !outRec.isOpen && cnt < 3) {
				continue
			}
			outRec.fixHoleLinkage()
			val pn = PolyNode()
			polytree.allPolys.add(pn)
			outRec.polyNode = pn
			var op: Path.OutPt = outRec.points!!.prev!!
			for (j in 0..cnt - 1) {
				pn.polygon.add(op.pt)
				op = op.prev!!
			}
		}

		//fixup PolyNode links etc ...
		for (i in polyOuts.indices) {
			val outRec = polyOuts[i]
			if (outRec.polyNode == null) {
				continue
			} else if (outRec.isOpen) {
				outRec.polyNode!!.isOpen = true
				polytree.addChild(outRec.polyNode!!)
			} else if (outRec.firstLeft != null && outRec.firstLeft!!.polyNode != null) {
				outRec.firstLeft!!.polyNode!!.addChild(outRec.polyNode!!)
			} else {
				polytree.addChild(outRec.polyNode!!)
			}
		}
	}

	private fun copyAELToSEL() {
		var e = activeEdges
		sortedEdges = e
		while (e != null) {
			e.prevInSEL = e.prevInAEL
			e.nextInSEL = e.nextInAEL
			e = e.nextInAEL
		}
	}

	private fun createOutRec(): Path.OutRec {
		val result = Path.OutRec()
		result.Idx = Edge.UNASSIGNED
		result.isHole = false
		result.isOpen = false
		result.firstLeft = null
		result.points = null
		result.bottomPt = null
		result.polyNode = null
		polyOuts.add(result)
		result.Idx = polyOuts.size - 1
		return result
	}

	private fun deleteFromAEL(e: Edge) {
		val AelPrev = e.prevInAEL
		val AelNext = e.nextInAEL
		if (AelPrev == null && AelNext == null && e !== activeEdges) {
			return  //already deleted
		}
		if (AelPrev != null) {
			AelPrev.nextInAEL = AelNext
		} else {
			activeEdges = AelNext
		}
		if (AelNext != null) {
			AelNext.prevInAEL = AelPrev
		}
		e.nextInAEL = null
		e.prevInAEL = null
	}

	private fun deleteFromSEL(e: Edge) {
		val SelPrev = e.prevInSEL
		val SelNext = e.nextInSEL
		if (SelPrev == null && SelNext == null && e != sortedEdges) {
			return  //already deleted
		}
		if (SelPrev != null) {
			SelPrev.nextInSEL = SelNext
		} else {
			sortedEdges = SelNext
		}
		if (SelNext != null) {
			SelNext.prevInSEL = SelPrev
		}
		e.nextInSEL = null
		e.prevInSEL = null
	}

	private fun doHorzSegmentsOverlap(seg1a: Double, seg1b: Double, seg2a: Double, seg2b: Double): Boolean {
		var _seg1a = seg1a
		var _seg1b = seg1b
		var _seg2a = seg2a
		var _seg2b = seg2b
		if (_seg1a > _seg1b) {
			val tmp = _seg1a
			_seg1a = _seg1b
			_seg1b = tmp
		}
		if (_seg2a > _seg2b) {
			val tmp = _seg2a
			_seg2a = _seg2b
			_seg2b = tmp
		}
		return _seg1a < _seg2b && _seg2a < _seg1b
	}

	private fun doMaxima(e: Edge) {
		val eMaxPair = e.maximaPair
		if (eMaxPair == null) {
			if (e.outIdx >= 0) {
				addOutPt(e, e.top)
			}
			deleteFromAEL(e)
			return
		}

		var eNext = e.nextInAEL
		while (eNext != null && eNext !== eMaxPair) {
			val tmp = Point2d(e.top)
			intersectEdges(e, eNext, tmp)
			e.top = Point2d(tmp)
			swapPositionsInAEL(e, eNext)
			eNext = e.nextInAEL
		}

		if (e.outIdx == Edge.UNASSIGNED && eMaxPair.outIdx == Edge.UNASSIGNED) {
			deleteFromAEL(e)
			deleteFromAEL(eMaxPair)
		} else if (e.outIdx >= 0 && eMaxPair.outIdx >= 0) {
			if (e.outIdx >= 0) {
				addLocalMaxPoly(e, eMaxPair, e.top)
			}
			deleteFromAEL(e)
			deleteFromAEL(eMaxPair)
		} else if (e.windDelta == 0) {
			if (e.outIdx >= 0) {
				addOutPt(e, e.top)
				e.outIdx = Edge.UNASSIGNED
			}
			deleteFromAEL(e)

			if (eMaxPair.outIdx >= 0) {
				addOutPt(eMaxPair, e.top)
				eMaxPair.outIdx = Edge.UNASSIGNED
			}
			deleteFromAEL(eMaxPair)
		} else {
			throw IllegalStateException("DoMaxima error")
		}
	}

	//------------------------------------------------------------------------------

	private fun doSimplePolygons() {
		var i = 0
		while (i < polyOuts.size) {
			val outrec = polyOuts[i++]
			var op: Path.OutPt? = outrec.points
			if (op == null || outrec.isOpen) {
				continue
			}
			do
			//for each Pt in Polygon until duplicate found do ...
			{
				var op2: Path.OutPt = op!!.next!!
				while (op2 !== outrec.points) {
					if (op.pt == op2.pt && op2.next != op && op2.prev != op) {
						//split the polygon into two ...
						val op3 = op.prev
						val op4 = op2.prev
						op.prev = op4
						op4!!.next = op
						op2.prev = op3
						op3!!.next = op2

						outrec.points = op
						val outrec2 = createOutRec()
						outrec2.points = op2
						updateOutPtIdxs(outrec2)
						if (poly2ContainsPoly1(outrec2.points!!, outrec.points!!)) {
							//OutRec2 is contained by OutRec1 ...
							outrec2.isHole = !outrec.isHole
							outrec2.firstLeft = outrec
							if (usingPolyTree) {
								fixupFirstLefts2(outrec2, outrec)
							}
						} else if (poly2ContainsPoly1(outrec.points!!, outrec2.points!!)) {
							//OutRec1 is contained by OutRec2 ...
							outrec2.isHole = outrec.isHole
							outrec.isHole = !outrec2.isHole
							outrec2.firstLeft = outrec.firstLeft
							outrec.firstLeft = outrec2
							if (usingPolyTree) {
								fixupFirstLefts2(outrec, outrec2)
							}
						} else {
							//the 2 polygons are separate ...
							outrec2.isHole = outrec.isHole
							outrec2.firstLeft = outrec.firstLeft
							if (usingPolyTree) {
								fixupFirstLefts1(outrec, outrec2)
							}
						}
						op2 = op //ie get ready for the next iteration
					}
					op2 = op2.next!!
				}
				op = op.next
			} while (op !== outrec.points)
		}
	}

	//------------------------------------------------------------------------------

	private fun EdgesAdjacent(inode: IntersectNode): Boolean {
		return inode.edge1!!.nextInSEL === inode.Edge2 || inode.edge1!!.prevInSEL === inode.Edge2
	}

	//------------------------------------------------------------------------------

	override fun execute(clipType: Clipper.ClipType, solution: Paths): Boolean {
		return execute(clipType, solution, Clipper.PolyFillType.EVEN_ODD, Clipper.PolyFillType.EVEN_ODD)
	}

	override fun execute(clipType: Clipper.ClipType, solution: Paths, subjFillType: Clipper.PolyFillType, clipFillType: Clipper.PolyFillType): Boolean {

		return synchronized(this) {

			if (hasOpenPaths) {
				throw IllegalStateException("Error: PolyTree struct is need for open path clipping.")
			}

			solution.clear()
			this.subjFillType = subjFillType
			this.clipFillType = clipFillType
			this.clipType = clipType
			usingPolyTree = false
			val succeeded: Boolean
			try {
				succeeded = executeInternal()
				//build the return polygons ...
				if (succeeded) {
					buildResult(solution)
				}
				return@synchronized succeeded
			} finally {
				polyOuts.clear()

			}
		}

	}

	override fun execute(clipType: Clipper.ClipType, polytree: PolyTree): Boolean {
		return execute(clipType, polytree, Clipper.PolyFillType.EVEN_ODD, Clipper.PolyFillType.EVEN_ODD)
	}

	override fun execute(clipType: Clipper.ClipType, polytree: PolyTree, subjFillType: Clipper.PolyFillType, clipFillType: Clipper.PolyFillType): Boolean {
		return synchronized(this) {
			this.subjFillType = subjFillType
			this.clipFillType = clipFillType
			this.clipType = clipType
			usingPolyTree = true
			val succeeded: Boolean
			try {
				succeeded = executeInternal()
				//build the return polygons ...
				if (succeeded) {
					buildResult2(polytree)
				}
			} finally {
				polyOuts.clear()
			}
			return@synchronized succeeded
		}
	}

	//------------------------------------------------------------------------------

	private fun executeInternal(): Boolean {
		try {
			reset()
			if (currentLM == null) {
				return false
			}

			var botY = popScanbeam()

			do {
				insertLocalMinimaIntoAEL(botY)
				ghostJoins.clear()
				processHorizontals(false)
				if (scanbeam == null) {
					break
				}
				val topY = popScanbeam()
				if (!processIntersections(topY)) {
					return false
				}
				processEdgesAtTopOfScanbeam(topY)
				botY = topY
			} while (scanbeam != null || currentLM != null)

			for (outRec in polyOuts) {
				if (outRec.points == null || outRec.isOpen) continue
			}
			//fix orientations ...
			for (outRec in polyOuts) {
				if (outRec.points == null || outRec.isOpen) continue
				if (outRec.isHole xor reverseSolution == outRec.area() > 0) {
					outRec.points!!.reversePolyPtLinks()
				}
			}

			joinCommonEdges()

			for (i in polyOuts.indices) {
				val outRec = polyOuts[i]
				if (outRec.points != null && !outRec.isOpen) {
					fixupOutPolygon(outRec)
				}
			}

			if (strictlySimple) {
				doSimplePolygons()
			}
			return true
		} finally {
			joins.clear()
			ghostJoins.clear()
		}//catch { return false; }
	}

	//------------------------------------------------------------------------------

	private fun fixupFirstLefts1(OldOutRec: Path.OutRec, NewOutRec: Path.OutRec) {
		for (i in polyOuts.indices) {
			val outRec = polyOuts[i]
			if (outRec.points == null || outRec.firstLeft == null) {
				continue
			}
			val firstLeft = outRec.firstLeft!!.parseFirstLeft()
			if (firstLeft == OldOutRec) {
				if (poly2ContainsPoly1(outRec.points!!, NewOutRec.points!!)) {
					outRec.firstLeft = NewOutRec
				}
			}
		}
	}

	private fun fixupFirstLefts2(OldOutRec: Path.OutRec, NewOutRec: Path.OutRec) {
		for (outRec in polyOuts) {
			if (outRec.firstLeft == OldOutRec) {
				outRec.firstLeft = NewOutRec
			}
		}
	}

	private fun fixupIntersectionOrder(): Boolean {
		//pre-condition: intersections are sorted bottom-most first.
		//Now it's crucial that intersections are made only between adjacent edges,
		//so to ensure this the order of intersections may need adjusting ...
		intersectList.sortWith(intersectNodeComparer)

		copyAELToSEL()
		val cnt = intersectList.size
		for (i in 0..cnt - 1) {
			if (!EdgesAdjacent(intersectList[i])) {
				var j = i + 1
				while (j < cnt && !EdgesAdjacent(intersectList[j])) {
					j++
				}
				if (j == cnt) {
					return false
				}

				val tmp = intersectList[i]
				intersectList[i] = intersectList[j]
				intersectList[j] = tmp

			}
			swapPositionsInSEL(intersectList[i].edge1!!, intersectList[i].Edge2!!)
		}
		return true
	}

	//----------------------------------------------------------------------

	private fun fixupOutPolygon(outRec: Path.OutRec) {
		//FixupOutPolygon() - removes duplicate points and simplifies consecutive
		//parallel edges by removing the middle vertex.
		var lastOK: Path.OutPt? = null
		outRec.bottomPt = null
		var pp: Path.OutPt = outRec.points!!
		while (true) {
			if (pp.prev === pp || pp.prev === pp.next) {
				outRec.points = null
				return
			}
			//test for duplicate points and collinear edges ...
			if (pp.pt == pp.next!!.pt || pp.pt == pp.prev!!.pt
				|| Points.slopesEqual(pp.prev!!.pt, pp.pt, pp.next!!.pt) && (!isPreserveCollinear || !Points.isPt2BetweenPt1AndPt3(pp.prev!!.pt, pp.pt, pp.next!!.pt))) {
				lastOK = null
				pp.prev!!.next = pp.next
				pp.next!!.prev = pp.prev
				pp = pp.prev!!
			} else if (pp === lastOK) {
				break
			} else {
				if (lastOK == null) {
					lastOK = pp
				}
				pp = pp.next!!
			}
		}
		outRec.points = pp
	}

	private fun getOutRec(idx: Int): Path.OutRec {
		var outrec = polyOuts[idx]
		while (outrec !== polyOuts[outrec.Idx]) {
			outrec = polyOuts[outrec.Idx]
		}
		return outrec
	}

	private fun insertEdgeIntoAEL(edge: Edge, startEdge: Edge?) {
		var sedge = startEdge

		if (activeEdges == null) {
			edge.prevInAEL = null
			edge.nextInAEL = null
			activeEdges = edge
		} else if (sedge == null && Edge.doesE2InsertBeforeE1(activeEdges!!, edge)) {
			edge.prevInAEL = null
			edge.nextInAEL = activeEdges
			activeEdges!!.prevInAEL = edge
			activeEdges = edge
		} else {
			if (sedge == null) sedge = activeEdges
			while (sedge!!.nextInAEL != null && !Edge.doesE2InsertBeforeE1(sedge.nextInAEL!!, edge)) sedge = sedge.nextInAEL
			edge.nextInAEL = sedge.nextInAEL
			if (sedge.nextInAEL != null) sedge.nextInAEL!!.prevInAEL = edge
			edge.prevInAEL = sedge
			sedge.nextInAEL = edge
		}
	}

	//------------------------------------------------------------------------------

	private fun insertLocalMinimaIntoAEL(botY: Double) {
		while (currentLM != null && currentLM!!.y == botY) {
			val lb = currentLM!!.leftBound
			val rb = currentLM!!.rightBound
			popLocalMinima()

			var Op1: Path.OutPt? = null
			if (lb == null) {
				insertEdgeIntoAEL(rb!!, null)
				updateWindingCount(rb)
				if (rb.isContributing(clipFillType!!, subjFillType!!, clipType!!)) {
					Op1 = addOutPt(rb, rb.bot)
				}
			} else if (rb == null) {
				insertEdgeIntoAEL(lb, null)
				updateWindingCount(lb)
				if (lb.isContributing(clipFillType!!, subjFillType!!, clipType!!)) {
					Op1 = addOutPt(lb, lb.bot)
				}
				insertScanbeam(lb.top.y)
			} else {
				insertEdgeIntoAEL(lb, null)
				insertEdgeIntoAEL(rb, lb)
				updateWindingCount(lb)
				rb.windCnt = lb.windCnt
				rb.windCnt2 = lb.windCnt2
				if (lb.isContributing(clipFillType!!, subjFillType!!, clipType!!)) {
					Op1 = addLocalMinPoly(lb, rb, lb.bot)
				}
				insertScanbeam(lb.top.y)
			}

			if (rb != null) {
				if (rb.isHorizontal) {
					addEdgeToSEL(rb)
				} else {
					insertScanbeam(rb.top.y)
				}
			}

			if (lb == null || rb == null) {
				continue
			}

			//if output polygons share an Edge with a horizontal rb, they'll need joining later ...
			if (Op1 != null && rb.isHorizontal && ghostJoins.size > 0 && rb.windDelta != 0) {
				for (i in ghostJoins.indices) {
					//if the horizontal Rb and a 'ghost' horizontal overlap, then convert
					//the 'ghost' join to a real join ready for later ...
					val j = ghostJoins[i]
					if (doHorzSegmentsOverlap(j.outPt1!!.pt.x, j.offPt!!.x, rb.bot.x, rb.top.x)) {
						addJoin(j.outPt1!!, Op1, j.offPt!!)
					}
				}
			}

			if (lb.outIdx >= 0 && lb.prevInAEL != null && lb.prevInAEL!!.current.x == lb.bot.x && lb.prevInAEL!!.outIdx >= 0
				&& Edge.slopesEqual(lb.prevInAEL!!, lb) && lb.windDelta != 0 && lb.prevInAEL!!.windDelta != 0) {
				val Op2 = addOutPt(lb.prevInAEL!!, lb.bot)
				addJoin(Op1!!, Op2, lb.top)
			}

			if (lb.nextInAEL !== rb) {

				if (rb.outIdx >= 0 && rb.prevInAEL!!.outIdx >= 0 && Edge.slopesEqual(rb.prevInAEL!!, rb) && rb.windDelta != 0 && rb.prevInAEL!!.windDelta != 0) {
					val Op2 = addOutPt(rb.prevInAEL!!, rb.bot)
					addJoin(Op1!!, Op2, rb.top)
				}

				var e = lb.nextInAEL
				if (e != null) {
					while (e !== rb) {
						//nb: For calculating winding counts etc, IntersectEdges() assumes
						//that param1 will be to the right of param2 ABOVE the intersection ...
						//nb: For calculating winding counts etc, IntersectEdges() assumes
						//that param1 will be to the right of param2 ABOVE the intersection ...
						intersectEdges(rb, e!!, lb.current) //order important here
						e = e.nextInAEL

					}
				}
			}
		}
	}

	//------------------------------------------------------------------------------

	private fun insertScanbeam(y: Double) {
		if (scanbeam == null) {
			scanbeam = Scanbeam()
			scanbeam!!.next = null
			scanbeam!!.y = y
		} else if (y > scanbeam!!.y) {
			val newSb = Scanbeam()
			newSb.y = y
			newSb.next = scanbeam
			scanbeam = newSb
		} else {
			var sb2: Scanbeam = scanbeam!!
			while (sb2.next != null && y <= sb2.next!!.y) {
				sb2 = sb2.next!!
			}
			if (y == sb2.y) {
				return  //ie ignores duplicates
			}
			val newSb = Scanbeam()
			newSb.y = y
			newSb.next = sb2.next
			sb2.next = newSb
		}
	}

	//------------------------------------------------------------------------------

	private fun intersectEdges(e1: Edge, e2: Edge, pt: Point2d) {
		//e1 will be to the left of e2 BELOW the intersection. Therefore e1 is before
		//e2 in AEL except when e1 is being inserted at the intersection point ...

		val e1Contributing = e1.outIdx >= 0
		val e2Contributing = e2.outIdx >= 0

		setZ(pt, e1, e2)

		//if either edge is on an OPEN path ...
		if (e1.windDelta == 0 || e2.windDelta == 0) {
			//ignore subject-subject open path intersections UNLESS they
			//are both open paths, AND they are both 'contributing maximas' ...
			if (e1.windDelta == 0 && e2.windDelta == 0) {
				return
			} else if (e1.polyTyp == e2.polyTyp && e1.windDelta != e2.windDelta && clipType == Clipper.ClipType.UNION) {
				if (e1.windDelta == 0) {
					if (e2Contributing) {
						addOutPt(e1, pt)
						if (e1Contributing) {
							e1.outIdx = Edge.UNASSIGNED
						}
					}
				} else {
					if (e1Contributing) {
						addOutPt(e2, pt)
						if (e2Contributing) {
							e2.outIdx = Edge.UNASSIGNED
						}
					}
				}
			} else if (e1.polyTyp != e2.polyTyp) {
				if (e1.windDelta == 0 && abs(e2.windCnt) == 1 && (clipType != Clipper.ClipType.UNION || e2.windCnt2 == 0)) {
					addOutPt(e1, pt)
					if (e1Contributing) {
						e1.outIdx = Edge.UNASSIGNED
					}
				} else if (e2.windDelta == 0 && abs(e1.windCnt) == 1 && (clipType != Clipper.ClipType.UNION || e1.windCnt2 == 0)) {
					addOutPt(e2, pt)
					if (e2Contributing) {
						e2.outIdx = Edge.UNASSIGNED
					}
				}
			}
			return
		}

		//update winding counts...
		//assumes that e1 will be to the Right of e2 ABOVE the intersection
		if (e1.polyTyp == e2.polyTyp) {
			if (e1.isEvenOddFillType(clipFillType!!, subjFillType!!)) {
				val oldE1WindCnt = e1.windCnt
				e1.windCnt = e2.windCnt
				e2.windCnt = oldE1WindCnt
			} else {
				if (e1.windCnt + e2.windDelta == 0) {
					e1.windCnt = -e1.windCnt
				} else {
					e1.windCnt += e2.windDelta
				}
				if (e2.windCnt - e1.windDelta == 0) {
					e2.windCnt = -e2.windCnt
				} else {
					e2.windCnt -= e1.windDelta
				}
			}
		} else {
			if (!e2.isEvenOddFillType(clipFillType!!, subjFillType!!)) {
				e1.windCnt2 += e2.windDelta
			} else {
				e1.windCnt2 = if (e1.windCnt2 == 0) 1 else 0
			}
			if (!e1.isEvenOddFillType(clipFillType!!, subjFillType!!)) {
				e2.windCnt2 -= e1.windDelta
			} else {
				e2.windCnt2 = if (e2.windCnt2 == 0) 1 else 0
			}
		}

		val e1FillType: Clipper.PolyFillType
		val e2FillType: Clipper.PolyFillType
		val e1FillType2: Clipper.PolyFillType
		val e2FillType2: Clipper.PolyFillType
		if (e1.polyTyp == Clipper.PolyType.SUBJECT) {
			e1FillType = subjFillType!!
			e1FillType2 = clipFillType!!
		} else {
			e1FillType = clipFillType!!
			e1FillType2 = subjFillType!!
		}
		if (e2.polyTyp == Clipper.PolyType.SUBJECT) {
			e2FillType = subjFillType!!
			e2FillType2 = clipFillType!!
		} else {
			e2FillType = clipFillType!!
			e2FillType2 = subjFillType!!
		}

		val e1Wc: Int
		val e2Wc: Int
		when (e1FillType) {
			Clipper.PolyFillType.POSITIVE -> e1Wc = e1.windCnt
			Clipper.PolyFillType.NEGATIVE -> e1Wc = -e1.windCnt
			else -> e1Wc = abs(e1.windCnt)
		}
		when (e2FillType) {
			Clipper.PolyFillType.POSITIVE -> e2Wc = e2.windCnt
			Clipper.PolyFillType.NEGATIVE -> e2Wc = -e2.windCnt
			else -> e2Wc = abs(e2.windCnt)
		}

		if (e1Contributing && e2Contributing) {
			if (e1Wc != 0 && e1Wc != 1 || e2Wc != 0 && e2Wc != 1 || e1.polyTyp != e2.polyTyp && clipType != Clipper.ClipType.XOR) {
				addLocalMaxPoly(e1, e2, pt)
			} else {
				addOutPt(e1, pt)
				addOutPt(e2, pt)
				Edge.swapSides(e1, e2)
				Edge.swapPolyIndexes(e1, e2)
			}
		} else if (e1Contributing) {
			if (e2Wc == 0 || e2Wc == 1) {
				addOutPt(e1, pt)
				Edge.swapSides(e1, e2)
				Edge.swapPolyIndexes(e1, e2)
			}

		} else if (e2Contributing) {
			if (e1Wc == 0 || e1Wc == 1) {
				addOutPt(e2, pt)
				Edge.swapSides(e1, e2)
				Edge.swapPolyIndexes(e1, e2)
			}
		} else if ((e1Wc == 0 || e1Wc == 1) && (e2Wc == 0 || e2Wc == 1)) {
			//neither edge is currently contributing ...
			val e1Wc2: Int
			val e2Wc2: Int
			when (e1FillType2) {
				Clipper.PolyFillType.POSITIVE -> e1Wc2 = e1.windCnt2
				Clipper.PolyFillType.NEGATIVE -> e1Wc2 = -e1.windCnt2
				else -> e1Wc2 = abs(e1.windCnt2)
			}
			when (e2FillType2) {
				Clipper.PolyFillType.POSITIVE -> e2Wc2 = e2.windCnt2
				Clipper.PolyFillType.NEGATIVE -> e2Wc2 = -e2.windCnt2
				else -> e2Wc2 = abs(e2.windCnt2)
			}

			if (e1.polyTyp != e2.polyTyp) {
				addLocalMinPoly(e1, e2, pt)
			} else if (e1Wc == 1 && e2Wc == 1) {
				when (clipType) {
					Clipper.ClipType.INTERSECTION -> if (e1Wc2 > 0 && e2Wc2 > 0) {
						addLocalMinPoly(e1, e2, pt)
					}
					Clipper.ClipType.UNION -> if (e1Wc2 <= 0 && e2Wc2 <= 0) {
						addLocalMinPoly(e1, e2, pt)
					}
					Clipper.ClipType.DIFFERENCE -> if (e1.polyTyp == Clipper.PolyType.CLIP && e1Wc2 > 0 && e2Wc2 > 0 || e1.polyTyp == Clipper.PolyType.SUBJECT && e1Wc2 <= 0 && e2Wc2 <= 0) {
						addLocalMinPoly(e1, e2, pt)
					}
					Clipper.ClipType.XOR -> addLocalMinPoly(e1, e2, pt)
				}
			} else {
				Edge.swapSides(e1, e2)
			}
		}
	}

	private fun intersectPoint(edge1: Edge, edge2: Edge, ipV: Array<Point2d>) {
		ipV[0] = Point2d()
		val ip = ipV[0]

		val b1: Double
		val b2: Double
		//nb: with very large coordinate values, it's possible for SlopesEqual() to
		//return false but for the edge.Dx value be equal due to double precision rounding.
		if (edge1.deltaX == edge2.deltaX) {
			ip.y = edge1.current.y
			ip.x = Edge.topX(edge1, ip.y)
			return
		}

		if (edge1.delta.x == 0.0) {
			ip.x = edge1.bot.x
			if (edge2.isHorizontal) {
				ip.y = edge2.bot.y
			} else {
				b2 = edge2.bot.y - edge2.bot.x / edge2.deltaX
				ip.y = round(ip.x / edge2.deltaX + b2).toDouble()
			}
		} else if (edge2.delta.x == 0.0) {
			ip.x = edge2.bot.x
			if (edge1.isHorizontal) {
				ip.y = edge1.bot.y
			} else {
				b1 = edge1.bot.y - edge1.bot.x / edge1.deltaX
				ip.y = round(ip.x / edge1.deltaX + b1).toDouble()
			}
		} else {
			b1 = edge1.bot.x - edge1.bot.y * edge1.deltaX
			b2 = edge2.bot.x - edge2.bot.y * edge2.deltaX
			val q = (b2 - b1) / (edge1.deltaX - edge2.deltaX)
			ip.y = round(q).toDouble()
			if (abs(edge1.deltaX) < abs(edge2.deltaX)) {
				ip.x = round(edge1.deltaX * q + b1).toDouble()
			} else {
				ip.x = round(edge2.deltaX * q + b2).toDouble()
			}
		}

		if (ip.y < edge1.top.y || ip.y < edge2.top.y) {
			if (edge1.top.y > edge2.top.y) {
				ip.y = edge1.top.y
			} else {
				ip.y = edge2.top.y
			}
			if (abs(edge1.deltaX) < abs(edge2.deltaX)) {
				ip.x = Edge.topX(edge1, ip.y)
			} else {
				ip.x = Edge.topX(edge2, ip.y)
			}
		}
		//finally, don't allow 'ip' to be BELOW curr.getY() (ie bottom of scanbeam) ...
		if (ip.y > edge1.current.y) {
			ip.y = edge1.current.y
			//better to use the more vertical edge to derive X ...
			if (abs(edge1.deltaX) > abs(edge2.deltaX)) {
				ip.x = Edge.topX(edge2, ip.y)
			} else {
				ip.x = Edge.topX(edge1, ip.y)
			}
		}
	}

	private fun joinCommonEdges() {
		for (i in joins.indices) {
			val join = joins[i]

			val outRec1 = getOutRec(join.outPt1!!.idx)
			var outRec2 = getOutRec(join.outPt2!!.idx)

			if (outRec1.points == null || outRec2.points == null) {
				continue
			}

			//get the polygon fragment with the correct hole state (FirstLeft)
			//before calling JoinPoints() ...
			val holeStateRec: Path.OutRec
			if (outRec1 === outRec2) {
				holeStateRec = outRec1
			} else if (isParam1RightOfParam2(outRec1, outRec2)) {
				holeStateRec = outRec2
			} else if (isParam1RightOfParam2(outRec2, outRec1)) {
				holeStateRec = outRec1
			} else {
				holeStateRec = Path.OutPt.getLowerMostRec(outRec1, outRec2)
			}

			if (!joinPoints(join, outRec1, outRec2)) {
				continue
			}

			if (outRec1 === outRec2) {
				//instead of joining two polygons, we've just created a new one by
				//splitting one polygon into two.
				outRec1.points = join.outPt1
				outRec1.bottomPt = null
				outRec2 = createOutRec()
				outRec2.points = join.outPt2

				//update all OutRec2.Pts Idx's ...
				updateOutPtIdxs(outRec2)

				//We now need to check every OutRec.FirstLeft pointer. If it points
				//to OutRec1 it may need to point to OutRec2 instead ...
				if (usingPolyTree) {
					for (j in 0..polyOuts.size - 1 - 1) {
						val oRec = polyOuts[j]
						if (oRec.points == null || oRec.firstLeft!!.parseFirstLeft() !== outRec1 || oRec.isHole == outRec1.isHole) {
							continue
						}
						if (poly2ContainsPoly1(oRec.points!!, join.outPt2!!)) {
							oRec.firstLeft = outRec2
						}
					}
				}

				if (poly2ContainsPoly1(outRec2.points!!, outRec1.points!!)) {
					//outRec2 is contained by outRec1 ...
					outRec2.isHole = !outRec1.isHole
					outRec2.firstLeft = outRec1

					//fixup FirstLeft pointers that may need reassigning to OutRec1
					if (usingPolyTree) {
						fixupFirstLefts2(outRec2, outRec1)
					}

					if (outRec2.isHole xor reverseSolution == outRec2.area() > 0) {
						outRec2.points!!.reversePolyPtLinks()
					}

				} else if (poly2ContainsPoly1(outRec1.points!!, outRec2.points!!)) {
					//outRec1 is contained by outRec2 ...
					outRec2.isHole = outRec1.isHole
					outRec1.isHole = !outRec2.isHole
					outRec2.firstLeft = outRec1.firstLeft
					outRec1.firstLeft = outRec2

					//fixup FirstLeft pointers that may need reassigning to OutRec1
					if (usingPolyTree) {
						fixupFirstLefts2(outRec1, outRec2)
					}

					if (outRec1.isHole xor reverseSolution == outRec1.area() > 0) {
						outRec1.points!!.reversePolyPtLinks()
					}
				} else {
					//the 2 polygons are completely separate ...
					outRec2.isHole = outRec1.isHole
					outRec2.firstLeft = outRec1.firstLeft

					//fixup FirstLeft pointers that may need reassigning to OutRec2
					if (usingPolyTree) {
						fixupFirstLefts1(outRec1, outRec2)
					}
				}

			} else {
				//joined 2 polygons together ...

				outRec2.points = null
				outRec2.bottomPt = null
				outRec2.Idx = outRec1.Idx

				outRec1.isHole = holeStateRec.isHole
				if (holeStateRec === outRec2) {
					outRec1.firstLeft = outRec2.firstLeft
				}
				outRec2.firstLeft = outRec1

				//fixup FirstLeft pointers that may need reassigning to OutRec1
				if (usingPolyTree) {
					fixupFirstLefts2(outRec2, outRec1)
				}
			}
		}
	}

	private fun popScanbeam(): Double {
		val y = scanbeam!!.y
		scanbeam = scanbeam!!.next
		return y
	}

	private fun processEdgesAtTopOfScanbeam(topY: Double) {
		var e = activeEdges
		while (e != null) {
			//1. process maxima, treating them as if they're 'bent' horizontal edges,
			//   but exclude maxima with horizontal edges. nb: e can't be a horizontal.
			var IsMaximaEdge = e.isMaxima(topY)

			if (IsMaximaEdge) {
				val eMaxPair = e.maximaPair
				IsMaximaEdge = eMaxPair == null || !eMaxPair.isHorizontal
			}

			if (IsMaximaEdge) {
				val ePrev = e.prevInAEL
				doMaxima(e)
				if (ePrev == null) {
					e = activeEdges
				} else {
					e = ePrev.nextInAEL
				}
			} else {
				//2. promote horizontal edges, otherwise update Curr.getX() and Curr.getY() ...
				if (e.isIntermediate(topY) && e.nextInLML!!.isHorizontal) {
					val t = arrayOf<Edge>(e)
					updateEdgeIntoAEL(t)
					e = t[0]
					if (e.outIdx >= 0) {
						addOutPt(e, e.bot)
					}
					addEdgeToSEL(e)
				} else {
					e.current.x = Edge.topX(e, topY)
					e.current.y = topY
				}

				if (strictlySimple) {
					val ePrev = e.prevInAEL
					if (e.outIdx >= 0 && e.windDelta != 0 && ePrev != null && ePrev.outIdx >= 0 && ePrev.current.x == e.current.x
						&& ePrev.windDelta != 0) {
						val ip = Point2d(e.current)

						setZ(ip, ePrev, e)

						val op = addOutPt(ePrev, ip)
						val op2 = addOutPt(e, ip)
						addJoin(op, op2, ip) //StrictlySimple (type-3) join
					}
				}

				e = e.nextInAEL
			}
		}

		//3. Process horizontals at the Top of the scanbeam ...
		processHorizontals(true)

		//4. Promote intermediate vertices ...
		e = activeEdges
		while (e != null) {
			if (e.isIntermediate(topY)) {
				var op: Path.OutPt? = null
				if (e.outIdx >= 0) {
					op = addOutPt(e, e.top)
				}
				val t = arrayOf<Edge>(e)
				updateEdgeIntoAEL(t)
				e = t[0]

				//if output polygons share an edge, they'll need joining later ...
				val ePrev = e.prevInAEL
				val eNext = e.nextInAEL
				if (ePrev != null && ePrev.current.x == e.bot.x && ePrev.current.y == e.bot.y && op != null
					&& ePrev.outIdx >= 0 && ePrev.current.y > ePrev.top.y && Edge.slopesEqual(e, ePrev) && e.windDelta != 0
					&& ePrev.windDelta != 0) {
					val op2 = addOutPt(ePrev, e.bot)
					addJoin(op, op2, e.top)
				} else if (eNext != null && eNext.current.x == e.bot.x && eNext.current.y == e.bot.y && op != null
					&& eNext.outIdx >= 0 && eNext.current.y > eNext.top.y && Edge.slopesEqual(e, eNext) && e.windDelta != 0
					&& eNext.windDelta != 0) {
					val op2 = addOutPt(eNext, e.bot)
					addJoin(op, op2, e.top)
				}
			}
			e = e.nextInAEL
		}
	}

	private fun processHorizontal(horizontalEdge: Edge, isTopOfScanbeam: Boolean) {
		var horzEdge = horizontalEdge
		val dir = Array(1) { Clipper.Direction.LEFT_TO_RIGHT }
		val horzLeft = DoubleArray(1)
		val horzRight = DoubleArray(1)

		getHorzDirection(horzEdge, dir, horzLeft, horzRight)

		var eLastHorz = horzEdge
		var eMaxPair: Edge? = null
		while (eLastHorz.nextInLML != null && eLastHorz.nextInLML!!.isHorizontal) {
			eLastHorz = eLastHorz.nextInLML!!
		}
		if (eLastHorz.nextInLML == null) {
			eMaxPair = eLastHorz.maximaPair
		}

		while (true) {
			val IsLastHorz = horzEdge === eLastHorz
			var e: Edge? = horzEdge.getNextInAEL(dir[0])
			while (e != null) {
				//Break if we've got to the end of an intermediate horizontal edge ...
				//nb: Smaller Dx's are to the right of larger Dx's ABOVE the horizontal.
				if (e.current.x == horzEdge.top.x && horzEdge.nextInLML != null && e.deltaX < horzEdge.nextInLML!!.deltaX) {
					break
				}

				val eNext = e.getNextInAEL(dir[0]) //saves eNext for later

				if (dir[0] == Clipper.Direction.LEFT_TO_RIGHT && e.current.x <= horzRight[0] || dir[0] == Clipper.Direction.RIGHT_TO_LEFT && e.current.x >= horzLeft[0]) {
					//so far we're still in range of the horizontal Edge  but make sure
					//we're at the last of consec. horizontals when matching with eMaxPair
					if (e === eMaxPair && IsLastHorz) {
						if (horzEdge.outIdx >= 0) {
							val op1 = addOutPt(horzEdge, horzEdge.top)
							var eNextHorz = sortedEdges
							while (eNextHorz != null) {
								if (eNextHorz.outIdx >= 0 && doHorzSegmentsOverlap(horzEdge.bot.x, horzEdge.top.x, eNextHorz.bot.x, eNextHorz.top.x)) {
									val op2 = addOutPt(eNextHorz, eNextHorz.bot)
									addJoin(op2, op1, eNextHorz.top)
								}
								eNextHorz = eNextHorz.nextInSEL
							}
							addGhostJoin(op1, horzEdge.bot)
							addLocalMaxPoly(horzEdge, eMaxPair, horzEdge.top)
						}
						deleteFromAEL(horzEdge)
						deleteFromAEL(eMaxPair)
						return
					} else if (dir[0] == Clipper.Direction.LEFT_TO_RIGHT) {
						val Pt = Point2d(e.current.x, horzEdge.current.y)
						intersectEdges(horzEdge, e, Pt)
					} else {
						val Pt = Point2d(e.current.x, horzEdge.current.y)
						intersectEdges(e, horzEdge, Pt)
					}
					swapPositionsInAEL(horzEdge, e)
				} else if (dir[0] == Clipper.Direction.LEFT_TO_RIGHT && e.current.x >= horzRight[0] || dir[0] == Clipper.Direction.RIGHT_TO_LEFT && e.current.x <= horzLeft[0]) {
					break
				}
				e = eNext
			} //end while

			if (horzEdge.nextInLML != null && horzEdge.nextInLML!!.isHorizontal) {

				val t = arrayOf(horzEdge)
				updateEdgeIntoAEL(t)
				horzEdge = t[0]

				if (horzEdge.outIdx >= 0) {
					addOutPt(horzEdge, horzEdge.bot)
				}
				getHorzDirection(horzEdge, dir.filterNotNull().toTypedArray(), horzLeft, horzRight)
			} else {
				break
			}
		} //end for (;;)

		if (horzEdge.nextInLML != null) {
			if (horzEdge.outIdx >= 0) {
				val op1 = addOutPt(horzEdge, horzEdge.top)
				if (isTopOfScanbeam) {
					addGhostJoin(op1, horzEdge.bot)
				}
				val t = arrayOf(horzEdge)
				updateEdgeIntoAEL(t)
				horzEdge = t[0]

				if (horzEdge.windDelta == 0) {
					return
				}
				//nb: HorzEdge is no longer horizontal here
				val ePrev = horzEdge.prevInAEL
				val eNext = horzEdge.nextInAEL
				if (ePrev != null && ePrev.current.x == horzEdge.bot.x && ePrev.current.y == horzEdge.bot.y
					&& ePrev.windDelta != 0 && ePrev.outIdx >= 0 && ePrev.current.y > ePrev.top.y
					&& Edge.slopesEqual(horzEdge, ePrev)) {
					val op2 = addOutPt(ePrev, horzEdge.bot)
					addJoin(op1, op2, horzEdge.top)
				} else if (eNext != null && eNext.current.x == horzEdge.bot.x && eNext.current.y == horzEdge.bot.y
					&& eNext.windDelta != 0 && eNext.outIdx >= 0 && eNext.current.y > eNext.top.y
					&& Edge.slopesEqual(horzEdge, eNext)) {
					val op2 = addOutPt(eNext, horzEdge.bot)
					addJoin(op1, op2, horzEdge.top)
				}
			} else {
				val t = arrayOf(horzEdge)
				updateEdgeIntoAEL(t)
				@Suppress("UNUSED_VALUE")
				horzEdge = t[0]
			}
		} else {
			if (horzEdge.outIdx >= 0) {
				addOutPt(horzEdge, horzEdge.top)
			}
			deleteFromAEL(horzEdge)
		}
	}

	//------------------------------------------------------------------------------

	private fun processHorizontals(isTopOfScanbeam: Boolean) {
		var horzEdge = sortedEdges
		while (horzEdge != null) {
			deleteFromSEL(horzEdge)
			processHorizontal(horzEdge, isTopOfScanbeam)
			horzEdge = sortedEdges
		}
	}

	//------------------------------------------------------------------------------

	private fun processIntersections(topY: Double): Boolean {
		if (activeEdges == null) return true
		try {
			buildIntersectList(topY)
			if (intersectList.size == 0) return true
			if (intersectList.size == 1 || fixupIntersectionOrder()) processIntersectList() else return false
		} catch (e: Exception) {
			sortedEdges = null
			intersectList.clear()
			println(e)
			throw IllegalStateException("ProcessIntersections error")
		}

		sortedEdges = null
		return true
	}

	private fun processIntersectList() {
		for (i in intersectList.indices) {
			val iNode = intersectList[i]
			run {
				intersectEdges(iNode.edge1!!, iNode.Edge2!!, iNode.pt!!)
				swapPositionsInAEL(iNode.edge1!!, iNode.Edge2!!)
			}
		}
		intersectList.clear()
	}

	//------------------------------------------------------------------------------

	override fun reset() {
		super.reset()
		scanbeam = null
		activeEdges = null
		sortedEdges = null
		var lm: LocalMinima? = minimaList
		while (lm != null) {
			insertScanbeam(lm.y)
			lm = lm.next
		}
	}

	private fun setHoleState(e: Edge, outRec: Path.OutRec) {
		var isHole = false
		var e2 = e.prevInAEL
		while (e2 != null) {
			if (e2.outIdx >= 0 && e2.windDelta != 0) {
				isHole = !isHole
				if (outRec.firstLeft == null) {
					outRec.firstLeft = polyOuts[e2.outIdx]
				}
			}
			e2 = e2.prevInAEL
		}
		if (isHole) {
			outRec.isHole = true
		}
	}

	@Suppress("UNUSED_PARAMETER")
	private fun setZ(pt: Point2d, e1: Edge, e2: Edge) {
		//if (pt.z != 0L || zFillFunction == null) {
		//	return
		//} else if (pt == e1.bot) {
		//	pt.z = e1.bot.z
		//} else if (pt == e1.top) {
		//	pt.z = e1.top.z
		//} else if (pt == e2.bot) {
		//	pt.z = e2.bot.z
		//} else if (pt == e2.top) {
		//	pt.z = e2.top.z
		//} else {
		//	zFillFunction!!.zFill(e1.bot, e1.top, e2.bot, e2.top, pt)
		//}
	}

	private fun swapPositionsInAEL(edge1: Edge, edge2: Edge) {
		//check that one or other edge hasn't already been removed from AEL ...
		if (edge1.nextInAEL === edge1.prevInAEL || edge2.nextInAEL === edge2.prevInAEL) {
			return
		}

		if (edge1.nextInAEL === edge2) {
			val next = edge2.nextInAEL
			if (next != null) {
				next.prevInAEL = edge1
			}
			val prev = edge1.prevInAEL
			if (prev != null) {
				prev.nextInAEL = edge2
			}
			edge2.prevInAEL = prev
			edge2.nextInAEL = edge1
			edge1.prevInAEL = edge2
			edge1.nextInAEL = next
		} else if (edge2.nextInAEL === edge1) {
			val next = edge1.nextInAEL
			if (next != null) {
				next.prevInAEL = edge2
			}
			val prev = edge2.prevInAEL
			if (prev != null) {
				prev.nextInAEL = edge1
			}
			edge1.prevInAEL = prev
			edge1.nextInAEL = edge2
			edge2.prevInAEL = edge1
			edge2.nextInAEL = next
		} else {
			val next = edge1.nextInAEL
			val prev = edge1.prevInAEL
			edge1.nextInAEL = edge2.nextInAEL
			if (edge1.nextInAEL != null) {
				edge1.nextInAEL!!.prevInAEL = edge1
			}
			edge1.prevInAEL = edge2.prevInAEL
			if (edge1.prevInAEL != null) {
				edge1.prevInAEL!!.nextInAEL = edge1
			}
			edge2.nextInAEL = next
			if (edge2.nextInAEL != null) {
				edge2.nextInAEL!!.prevInAEL = edge2
			}
			edge2.prevInAEL = prev
			if (edge2.prevInAEL != null) {
				edge2.prevInAEL!!.nextInAEL = edge2
			}
		}

		if (edge1.prevInAEL == null) {
			activeEdges = edge1
		} else if (edge2.prevInAEL == null) {
			activeEdges = edge2
		}
	}

	//------------------------------------------------------------------------------;

	private fun swapPositionsInSEL(edge1: Edge, edge2: Edge) {
		if (edge1.nextInSEL == null && edge1.prevInSEL == null) return
		if (edge2.nextInSEL == null && edge2.prevInSEL == null) return

		if (edge1.nextInSEL === edge2) {
			val next = edge2.nextInSEL
			if (next != null) {
				next.prevInSEL = edge1
			}
			val prev = edge1.prevInSEL
			if (prev != null) {
				prev.nextInSEL = edge2
			}
			edge2.prevInSEL = prev
			edge2.nextInSEL = edge1
			edge1.prevInSEL = edge2
			edge1.nextInSEL = next
		} else if (edge2.nextInSEL === edge1) {
			val next = edge1.nextInSEL
			if (next != null) {
				next.prevInSEL = edge2
			}
			val prev = edge2.prevInSEL
			if (prev != null) {
				prev.nextInSEL = edge1
			}
			edge1.prevInSEL = prev
			edge1.nextInSEL = edge2
			edge2.prevInSEL = edge1
			edge2.nextInSEL = next
		} else {
			val next = edge1.nextInSEL
			val prev = edge1.prevInSEL
			edge1.nextInSEL = edge2.nextInSEL
			if (edge1.nextInSEL != null) {
				edge1.nextInSEL!!.prevInSEL = edge1
			}
			edge1.prevInSEL = edge2.prevInSEL
			if (edge1.prevInSEL != null) {
				edge1.prevInSEL!!.nextInSEL = edge1
			}
			edge2.nextInSEL = next
			if (edge2.nextInSEL != null) {
				edge2.nextInSEL!!.prevInSEL = edge2
			}
			edge2.prevInSEL = prev
			if (edge2.prevInSEL != null) {
				edge2.prevInSEL!!.nextInSEL = edge2
			}
		}

		if (edge1.prevInSEL == null) {
			sortedEdges = edge1
		} else if (edge2.prevInSEL == null) {
			sortedEdges = edge2
		}
	}

	private fun updateEdgeIntoAEL(eV: Array<Edge>) {
		var e = eV[0]
		if (e.nextInLML == null) {
			throw IllegalStateException("UpdateEdgeIntoAEL: invalid call")
		}
		val AelPrev = e.prevInAEL
		val AelNext = e.nextInAEL
		e.nextInLML!!.outIdx = e.outIdx
		if (AelPrev != null) {
			AelPrev.nextInAEL = e.nextInLML
		} else {
			activeEdges = e.nextInLML
		}
		if (AelNext != null) {
			AelNext.prevInAEL = e.nextInLML
		}
		e.nextInLML!!.side = e.side
		e.nextInLML!!.windDelta = e.windDelta
		e.nextInLML!!.windCnt = e.windCnt
		e.nextInLML!!.windCnt2 = e.windCnt2
		e = e.nextInLML!!
		eV[0] = e
		e.current = Point2d(e.bot)
		e.prevInAEL = AelPrev
		e.nextInAEL = AelNext
		if (!e.isHorizontal) {
			insertScanbeam(e.top.y)
		}
	}

	private fun updateOutPtIdxs(outrec: Path.OutRec) {
		var op: Path.OutPt = outrec.points!!
		do {
			op.idx = outrec.Idx
			op = op.prev!!
		} while (op !== outrec.points)
	}

	private fun updateWindingCount(edge: Edge) {
		var e = edge.prevInAEL
		//find the edge of the same polytype that immediately preceeds 'edge' in AEL
		while (e != null && (e.polyTyp != edge.polyTyp || e.windDelta == 0)) {
			e = e.prevInAEL
		}
		if (e == null) {
			edge.windCnt = if (edge.windDelta == 0) 1 else edge.windDelta
			edge.windCnt2 = 0
			e = activeEdges //ie get ready to calc WindCnt2
		} else if (edge.windDelta == 0 && clipType != Clipper.ClipType.UNION) {
			edge.windCnt = 1
			edge.windCnt2 = e.windCnt2
			e = e.nextInAEL //ie get ready to calc WindCnt2
		} else if (edge.isEvenOddFillType(clipFillType!!, subjFillType!!)) {
			//EvenOdd filling ...
			if (edge.windDelta == 0) {
				//are we inside a subj polygon ...
				var Inside = true
				var e2 = e.prevInAEL
				while (e2 != null) {
					if (e2.polyTyp == e.polyTyp && e2.windDelta != 0) {
						Inside = !Inside
					}
					e2 = e2.prevInAEL
				}
				edge.windCnt = if (Inside) 0 else 1
			} else {
				edge.windCnt = edge.windDelta
			}
			edge.windCnt2 = e.windCnt2
			e = e.nextInAEL //ie get ready to calc WindCnt2
		} else {
			//nonZero, Positive or Negative filling ...
			if (e.windCnt * e.windDelta < 0) {
				//prev edge is 'decreasing' WindCount (WC) toward zero
				//so we're outside the previous polygon ...
				if (abs(e.windCnt) > 1) {
					//outside prev poly but still inside another.
					//when reversing direction of prev poly use the same WC
					if (e.windDelta * edge.windDelta < 0) {
						edge.windCnt = e.windCnt
					} else {
						edge.windCnt = e.windCnt + edge.windDelta
					}
				} else {
					//now outside all polys of same polytype so set own WC ...
					edge.windCnt = if (edge.windDelta == 0) 1 else edge.windDelta
				}
			} else {
				//prev edge is 'increasing' WindCount (WC) away from zero
				//so we're inside the previous polygon ...
				if (edge.windDelta == 0) {
					edge.windCnt = if (e.windCnt < 0) e.windCnt - 1 else e.windCnt + 1
				} else if (e.windDelta * edge.windDelta < 0) {
					edge.windCnt = e.windCnt
				} else {
					edge.windCnt = e.windCnt + edge.windDelta
				}
			}
			edge.windCnt2 = e.windCnt2
			e = e.nextInAEL //ie get ready to calc WindCnt2
		}

		//update WindCnt2 ...
		if (edge.isEvenOddAltFillType(clipFillType!!, subjFillType!!)) {
			//EvenOdd filling ...
			while (e !== edge) {
				if (e!!.windDelta != 0) {
					edge.windCnt2 = if (edge.windCnt2 == 0) 1 else 0
				}
				e = e.nextInAEL
			}
		} else {
			//nonZero, Positive or Negative filling ...
			while (e !== edge) {
				edge.windCnt2 += e!!.windDelta
				e = e.nextInAEL
			}
		}
	}

	companion object {

		private fun getHorzDirection(HorzEdge: Edge, Dir: Array<Clipper.Direction>, Left: DoubleArray, Right: DoubleArray) {
			if (HorzEdge.bot.x < HorzEdge.top.x) {
				Left[0] = HorzEdge.bot.x
				Right[0] = HorzEdge.top.x
				Dir[0] = Clipper.Direction.LEFT_TO_RIGHT
			} else {
				Left[0] = HorzEdge.top.x
				Right[0] = HorzEdge.bot.x
				Dir[0] = Clipper.Direction.RIGHT_TO_LEFT
			}
		}

		private fun getOverlap(a1: Double, a2: Double, b1: Double, b2: Double, Left: DoubleArray, Right: DoubleArray): Boolean {
			if (a1 < a2) {
				if (b1 < b2) {
					Left[0] = Math.max(a1, b1)
					Right[0] = Math.min(a2, b2)
				} else {
					Left[0] = Math.max(a1, b2)
					Right[0] = Math.min(a2, b1)
				}
			} else {
				if (b1 < b2) {
					Left[0] = Math.max(a2, b1)
					Right[0] = Math.min(a1, b2)
				} else {
					Left[0] = Math.max(a2, b2)
					Right[0] = Math.min(a1, b1)
				}
			}
			return Left[0] < Right[0]
		}

		private fun isParam1RightOfParam2(outRec1: Path.OutRec?, outRec2: Path.OutRec): Boolean {
			var outr = outRec1
			do {
				outr = outr!!.firstLeft
				if (outr === outRec2) {
					return true
				}
			} while (outr != null)
			return false
		}

		private fun isPointInPolygon(pt: Point2d, opp: Path.OutPt): Int {
			var op = opp
			//returns 0 if false, +1 if true, -1 if pt ON polygon boundary
			//See "The Point in Polygon Problem for Arbitrary Polygons" by Hormann & Agathos
			//http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.88.5498&rep=rep1&type=pdf
			var result = 0
			val startOp = op
			val ptx = pt.x
			val pty = pt.y
			var poly0x = op.pt.x
			var poly0y = op.pt.y
			do {
				op = op.next!!
				val poly1x = op.pt.x
				val poly1y = op.pt.y

				if (poly1y == pty) {
					if (poly1x == ptx || poly0y == pty && poly1x > ptx == poly0x < ptx) return -1
				}
				if (poly0y < pty != poly1y < pty) {
					if (poly0x >= ptx) {
						if (poly1x > ptx) {
							result = 1 - result
						} else {
							val d = (poly0x - ptx) * (poly1y - pty) - (poly1x - ptx) * (poly0y - pty)
							if (d == 0.0) return -1
							if (d > 0 == poly1y > poly0y) result = 1 - result
						}
					} else {
						if (poly1x > ptx) {
							val d = (poly0x - ptx) * (poly1y - pty) - (poly1x - ptx) * (poly0y - pty)
							if (d == 0.0) return -1
							if (d > 0 == poly1y > poly0y) result = 1 - result
						}
					}
				}
				poly0x = poly1x
				poly0y = poly1y
			} while (startOp !== op)

			return result
		}

		//------------------------------------------------------------------------------
		private fun joinHorz(op_1: Path.OutPt, op_1b: Path.OutPt, op_2: Path.OutPt, op_2b: Path.OutPt, Pt: Point2d, DiscardLeft: Boolean): Boolean {
			var op1 = op_1
			var op1b = op_1b
			var op2 = op_2
			var op2b = op_2b
			val Dir1 = if (op1.pt.x > op1b.pt.x) Clipper.Direction.RIGHT_TO_LEFT else Clipper.Direction.LEFT_TO_RIGHT
			val Dir2 = if (op2.pt.x > op2b.pt.x) Clipper.Direction.RIGHT_TO_LEFT else Clipper.Direction.LEFT_TO_RIGHT
			if (Dir1 == Dir2) {
				return false
			}

			//When DiscardLeft, we want Op1b to be on the Left of Op1, otherwise we
			//want Op1b to be on the Right. (And likewise with Op2 and Op2b.)
			//So, to facilitate this while inserting Op1b and Op2b ...
			//when DiscardLeft, make sure we're AT or RIGHT of Pt before adding Op1b,
			//otherwise make sure we're AT or LEFT of Pt. (Likewise with Op2b.)
			if (Dir1 == Clipper.Direction.LEFT_TO_RIGHT) {
				while (op1.next!!.pt.x <= Pt.x && op1.next!!.pt.x >= op1.pt.x && op1.next!!.pt.y == Pt.y) op1 = op1.next!!
				if (DiscardLeft && op1.pt.x != Pt.x) op1 = op1.next!!
				op1b = op1.duplicate(!DiscardLeft)
				if (op1b.pt != Pt) {
					op1 = op1b
					op1.pt = Point2d(Pt)
					op1b = op1.duplicate(!DiscardLeft)
				}
			} else {
				while (op1.next!!.pt.x >= Pt.x && op1.next!!.pt.x <= op1.pt.x && op1.next!!.pt.y == Pt.y) op1 = op1.next!!
				if (!DiscardLeft && op1.pt.x != Pt.x) op1 = op1.next!!
				op1b = op1.duplicate(DiscardLeft)
				if (op1b.pt != Pt) {
					op1 = op1b
					op1.pt = Point2d(Pt)
					op1b = op1.duplicate(DiscardLeft)
				}
			}

			if (Dir2 == Clipper.Direction.LEFT_TO_RIGHT) {
				while (op2.next!!.pt.x <= Pt.x && op2.next!!.pt.x >= op2.pt.x && op2.next!!.pt.y == Pt.y) op2 = op2.next!!
				if (DiscardLeft && op2.pt.x != Pt.x) op2 = op2.next!!
				op2b = op2.duplicate(!DiscardLeft)
				if (op2b.pt != Pt) {
					op2 = op2b
					op2.pt = Point2d(Pt)
					op2b = op2.duplicate(!DiscardLeft)
				}
			} else {
				while (op2.next!!.pt.x >= Pt.x && op2.next!!.pt.x <= op2.pt.x && op2.next!!.pt.y == Pt.y) op2 = op2.next!!
				if (!DiscardLeft && op2.pt.x != Pt.x) op2 = op2.next!!
				op2b = op2.duplicate(DiscardLeft)
				if (op2b.pt != Pt) {
					op2 = op2b
					op2.pt = Point2d(Pt)
					op2b = op2.duplicate(DiscardLeft)
				}
			}

			if (Dir1 == Clipper.Direction.LEFT_TO_RIGHT == DiscardLeft) {
				op1.prev = op2
				op2.next = op1
				op1b.next = op2b
				op2b.prev = op1b
			} else {
				op1.next = op2
				op2.prev = op1
				op1b.prev = op2b
				op2b.next = op1b
			}
			return true
		}

		private fun joinPoints(j: Path.Join, outRec1: Path.OutRec, outRec2: Path.OutRec): Boolean {
			var op1: Path.OutPt = j.outPt1!!
			var op1b: Path.OutPt
			var op2: Path.OutPt = j.outPt2!!
			var op2b: Path.OutPt

			//There are 3 kinds of joins for output polygons ...
			//1. Horizontal joins where Join.OutPt1 & Join.OutPt2 are a vertices anywhere
			//along (horizontal) collinear edges (& Join.OffPt is on the same horizontal).
			//2. Non-horizontal joins where Join.OutPt1 & Join.OutPt2 are at the same
			//location at the Bottom of the overlapping segment (& Join.OffPt is above).
			//3. StrictlySimple joins where edges touch but are not collinear and where
			//Join.OutPt1, Join.OutPt2 & Join.OffPt all share the same point.
			val isHorizontal = j.outPt1!!.pt.y == j.offPt!!.y

			if (isHorizontal && j.offPt == j.outPt1!!.pt && j.offPt == j.outPt2!!.pt) {
				//Strictly Simple join ...
				if (outRec1 !== outRec2) return false
				op1b = j.outPt1!!.next!!
				while (op1b !== op1 && op1b.pt == j.offPt) op1b = op1b.next!!
				val reverse1 = op1b.pt.y > j.offPt!!.y
				op2b = j.outPt2!!.next!!
				while (op2b !== op2 && op2b.pt == j.offPt) op2b = op2b.next!!
				val reverse2 = op2b.pt.y > j.offPt!!.y
				if (reverse1 == reverse2) return false
				if (reverse1) {
					op1b = op1.duplicate(false)
					op2b = op2.duplicate(true)
					op1.prev = op2
					op2.next = op1
					op1b.next = op2b
					op2b.prev = op1b
					j.outPt1 = op1
					j.outPt2 = op1b
					return true
				} else {
					op1b = op1.duplicate(true)
					op2b = op2.duplicate(false)
					op1.next = op2
					op2.prev = op1
					op1b.prev = op2b
					op2b.next = op1b
					j.outPt1 = op1
					j.outPt2 = op1b
					return true
				}
			} else if (isHorizontal) {
				//treat horizontal joins differently to non-horizontal joins since with
				//them we're not yet sure where the overlapping is. OutPt1.Pt & OutPt2.Pt
				//may be anywhere along the horizontal edge.
				op1b = op1
				while (op1.prev!!.pt.y == op1.pt.y && op1.prev !== op1b && op1.prev !== op2) op1 = op1.prev!!
				while (op1b.next!!.pt.y == op1b.pt.y && op1b.next !== op1 && op1b.next !== op2) op1b = op1b.next!!
				if (op1b.next === op1 || op1b.next === op2) return false //a flat 'polygon'

				op2b = op2
				while (op2.prev!!.pt.y == op2.pt.y && op2.prev !== op2b && op2.prev !== op1b) op2 = op2.prev!!
				while (op2b.next!!.pt.y == op2b.pt.y && op2b.next !== op2 && op2b.next !== op1) op2b = op2b.next!!
				if (op2b.next === op2 || op2b.next === op1) return false //a flat 'polygon'

				val LeftV = DoubleArray(1)
				val RightV = DoubleArray(1)
				//Op1 -. Op1b & Op2 -. Op2b are the extremites of the horizontal edges
				if (!getOverlap(op1.pt.x, op1b.pt.x, op2.pt.x, op2b.pt.x, LeftV, RightV)) return false
				val Left = LeftV[0]
				val Right = RightV[0]

				//DiscardLeftSide: when overlapping edges are joined, a spike will created
				//which needs to be cleaned up. However, we don't want Op1 or Op2 caught up
				//on the discard Side as either may still be needed for other joins ...
				val Pt: Point2d
				val DiscardLeftSide: Boolean
				if (op1.pt.x in Left..Right) {
					Pt = Point2d(op1.pt)
					DiscardLeftSide = op1.pt.x > op1b.pt.x
				} else if (op2.pt.x in Left..Right) {
					Pt = Point2d(op2.pt)
					DiscardLeftSide = op2.pt.x > op2b.pt.x
				} else if (op1b.pt.x in Left..Right) {
					Pt = Point2d(op1b.pt)
					DiscardLeftSide = op1b.pt.x > op1.pt.x
				} else {
					Pt = Point2d(op2b.pt)
					DiscardLeftSide = op2b.pt.x > op2.pt.x
				}
				j.outPt1 = op1
				j.outPt2 = op2
				return joinHorz(op1, op1b, op2, op2b, Pt, DiscardLeftSide)
			} else {
				//nb: For non-horizontal joins ...
				//    1. Jr.OutPt1.getPt().getY() == Jr.OutPt2.getPt().getY()
				//    2. Jr.OutPt1.Pt > Jr.OffPt.getY()

				//make sure the polygons are correctly oriented ...
				op1b = op1.next!!
				while (op1b.pt == op1.pt && op1b !== op1) op1b = op1b.next!!
				val Reverse1 = op1b.pt.y > op1.pt.y || !Points.slopesEqual(op1.pt, op1b.pt, j.offPt!!)
				if (Reverse1) {
					op1b = op1.prev!!
					while (op1b.pt == op1.pt && op1b !== op1) op1b = op1b.prev!!
					if (op1b.pt.y > op1.pt.y || !Points.slopesEqual(op1.pt, op1b.pt, j.offPt!!)) return false
				}
				op2b = op2.next!!
				while (op2b.pt == op2.pt && op2b !== op2) op2b = op2b.next!!
				val Reverse2 = op2b.pt.y > op2.pt.y || !Points.slopesEqual(op2.pt, op2b.pt, j.offPt!!)
				if (Reverse2) {
					op2b = op2.prev!!
					while (op2b.pt == op2.pt && op2b !== op2) op2b = op2b.prev!!
					if (op2b.pt.y > op2.pt.y || !Points.slopesEqual(op2.pt, op2b.pt, j.offPt!!)) return false
				}

				if (op1b === op1 || op2b === op2 || op1b === op2b || outRec1 === outRec2 && Reverse1 == Reverse2) return false

				if (Reverse1) {
					op1b = op1.duplicate(false)
					op2b = op2.duplicate(true)
					op1.prev = op2
					op2.next = op1
					op1b.next = op2b
					op2b.prev = op1b
					j.outPt1 = op1
					j.outPt2 = op1b
					return true
				} else {
					op1b = op1.duplicate(true)
					op2b = op2.duplicate(false)
					op1.next = op2
					op2.prev = op1
					op1b.prev = op2b
					op2b.next = op1b
					j.outPt1 = op1
					j.outPt2 = op1b
					return true
				}
			}
		}

		private fun minkowski(pattern: Path, path: Path, IsSum: Boolean, IsClosed: Boolean): Paths {
			val delta = if (IsClosed) 1 else 0
			val polyCnt = pattern.size
			val pathCnt = path.size
			val result = Paths(pathCnt)
			if (IsSum) {
				for (i in 0 until pathCnt) {
					val p = Path(polyCnt)
					for (ip in pattern) p.add(Point2d(path[i].x + ip.x, path[i].y + ip.y))
					result.add(p)
				}
			} else {
				for (i in 0 until pathCnt) {
					val p = Path(polyCnt)
					for (ip in pattern) p.add(Point2d(path[i].x - ip.x, path[i].y - ip.y))
					result.add(p)
				}
			}

			val quads = Paths((pathCnt + delta) * (polyCnt + 1))
			for (i in 0 until pathCnt - 1 + delta) {
				for (j in 0 until polyCnt) {
					val quad = Path(4)
					quad.add(result[i % pathCnt][j % polyCnt])
					quad.add(result[(i + 1) % pathCnt][j % polyCnt])
					quad.add(result[(i + 1) % pathCnt][(j + 1) % polyCnt])
					quad.add(result[i % pathCnt][(j + 1) % polyCnt])
					if (!quad.orientation()) quad.reverse()
					quads.add(quad)
				}
			}
			return quads
		}

		fun minkowskiDiff(poly1: Path, poly2: Path): Paths {
			val paths = minkowski(poly1, poly2, false, true)
			val c = DefaultClipper()
			c.addPaths(paths, Clipper.PolyType.SUBJECT, true)
			c.execute(Clipper.ClipType.UNION, paths, Clipper.PolyFillType.NON_ZERO, Clipper.PolyFillType.NON_ZERO)
			return paths
		}

		fun minkowskiSum(pattern: Path, path: Path, pathIsClosed: Boolean): Paths {
			val paths = minkowski(pattern, path, true, pathIsClosed)
			val c = DefaultClipper()
			c.addPaths(paths, Clipper.PolyType.SUBJECT, true)
			c.execute(Clipper.ClipType.UNION, paths, Clipper.PolyFillType.NON_ZERO, Clipper.PolyFillType.NON_ZERO)
			return paths
		}

		fun minkowskiSum(pattern: Path, paths: Paths, pathIsClosed: Boolean): Paths {
			val solution = Paths()
			val c = DefaultClipper()
			for (i in paths.indices) {
				val tmp = minkowski(pattern, paths[i], true, pathIsClosed)
				c.addPaths(tmp, Clipper.PolyType.SUBJECT, true)
				if (pathIsClosed) {
					val path = paths[i].translatePath(pattern[0])
					c.addPath(path, Clipper.PolyType.CLIP, true)
				}
			}
			c.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.NON_ZERO, Clipper.PolyFillType.NON_ZERO)
			return solution
		}

		private fun poly2ContainsPoly1(outPt1: Path.OutPt, outPt2: Path.OutPt): Boolean {
			var op = outPt1
			do {
				//nb: PointInPolygon returns 0 if false, +1 if true, -1 if pt on polygon
				val res = isPointInPolygon(op.pt, outPt2)
				if (res >= 0) {
					return res > 0
				}
				op = op.next!!
			} while (op !== outPt1)
			return true
		}

		//@JvmOverloads fun simplifyPolygon(poly: Path, fillType: Clipper.PolyFillType = Clipper.PolyFillType.EVEN_ODD): Paths {
		fun simplifyPolygon(poly: Path, fillType: Clipper.PolyFillType = Clipper.PolyFillType.EVEN_ODD): Paths {
			val result = Paths()
			val c = DefaultClipper(Clipper.STRICTLY_SIMPLE)

			c.addPath(poly, Clipper.PolyType.SUBJECT, true)
			c.execute(Clipper.ClipType.UNION, result, fillType, fillType)
			return result
		}

		//@JvmOverloads fun simplifyPolygons(polys: Paths, fillType: Clipper.PolyFillType = Clipper.PolyFillType.EVEN_ODD): Paths {
		fun simplifyPolygons(polys: Paths, fillType: Clipper.PolyFillType = Clipper.PolyFillType.EVEN_ODD): Paths {
			val result = Paths()
			val c = DefaultClipper(Clipper.STRICTLY_SIMPLE)

			c.addPaths(polys, Clipper.PolyType.SUBJECT, true)
			c.execute(Clipper.ClipType.UNION, result, fillType, fillType)
			return result
		}
	}

}//------------------------------------------------------------------------------

class Edge {
	enum class Side { LEFT, RIGHT }

	var bot: Point2d = Point2d(); set(v) = run { field.copyFrom(v) }
	var current: Point2d = Point2d(); set(v) = run { field.copyFrom(v) }
	var top: Point2d = Point2d(); set(v) = run { field.copyFrom(v) }

	val delta: Point2d
	var deltaX: Double = 0.0

	var polyTyp: Clipper.PolyType? = null

	var side: Side = Side.LEFT

	var windDelta: Int = 0 //1 or -1 depending on winding direction

	var windCnt: Int = 0
	var windCnt2: Int = 0 //winding count of the opposite polytype
	var outIdx: Int = 0
	var next: Edge? = null
	var prev: Edge? = null
	var nextInLML: Edge? = null
	var nextInAEL: Edge? = null
	var prevInAEL: Edge? = null
	var nextInSEL: Edge? = null
	var prevInSEL: Edge? = null

	init {
		delta = Point2d()
		top = Point2d()
		bot = Point2d()
		current = Point2d()
	}

	fun findNextLocMin(): Edge {
		var e = this
		var e2: Edge
		while (true) {
			while (e.bot != e.prev!!.bot || e.current == e.top) e = e.next!!
			if (e.deltaX != HORIZONTAL && e.prev!!.deltaX != HORIZONTAL) break
			while (e.prev!!.deltaX == HORIZONTAL) e = e.prev!!
			e2 = e
			while (e.deltaX == HORIZONTAL) e = e.next!!
			if (e.top.y == e.prev!!.bot.y) continue //ie just an intermediate horz.
			if (e2.prev!!.bot.x < e.bot.x) e = e2
			break
		}
		return e
	}

	val maximaPair: Edge?
		get() {
			var result: Edge? = null
			if (next!!.top == top && next!!.nextInLML == null) {
				result = next
			} else if (prev!!.top == top && prev!!.nextInLML == null) {
				result = prev
			}
			if (result != null && (result.outIdx == SKIP || result.nextInAEL === result.prevInAEL && !result.isHorizontal)) {
				return null
			}
			return result
		}

	fun getNextInAEL(direction: Clipper.Direction): Edge? {
		return if (direction == Clipper.Direction.LEFT_TO_RIGHT) nextInAEL else prevInAEL
	}

	fun isContributing(clipFillType: Clipper.PolyFillType, subjFillType: Clipper.PolyFillType, clipType: Clipper.ClipType): Boolean {
		val pft: Clipper.PolyFillType
		val pft2: Clipper.PolyFillType
		if (polyTyp == Clipper.PolyType.SUBJECT) {
			pft = subjFillType
			pft2 = clipFillType
		} else {
			pft = clipFillType
			pft2 = subjFillType
		}

		when (pft) {
			Clipper.PolyFillType.EVEN_ODD -> if (windDelta == 0 && windCnt != 1) return false //return false if a subj line has been flagged as inside a subj polygon
			Clipper.PolyFillType.NON_ZERO -> if (abs(windCnt) != 1) return false
			Clipper.PolyFillType.POSITIVE -> if (windCnt != 1) return false
			else -> if (windCnt != -1) return false //PolyFillType.pftNegative
		}

		when (clipType) {
			Clipper.ClipType.INTERSECTION -> {
				when (pft2) {
					Clipper.PolyFillType.EVEN_ODD, Clipper.PolyFillType.NON_ZERO -> return windCnt2 != 0
					Clipper.PolyFillType.POSITIVE -> return windCnt2 > 0
					else -> return windCnt2 < 0
				}
			}
			Clipper.ClipType.UNION -> {
				when (pft2) {
					Clipper.PolyFillType.EVEN_ODD, Clipper.PolyFillType.NON_ZERO -> return windCnt2 == 0
					Clipper.PolyFillType.POSITIVE -> return windCnt2 <= 0
					else -> return windCnt2 >= 0
				}
			}
			Clipper.ClipType.DIFFERENCE -> {
				if (polyTyp == Clipper.PolyType.SUBJECT) {
					when (pft2) {
						Clipper.PolyFillType.EVEN_ODD, Clipper.PolyFillType.NON_ZERO -> return windCnt2 == 0
						Clipper.PolyFillType.POSITIVE -> return windCnt2 <= 0
						else -> return windCnt2 >= 0
					}
				} else {
					when (pft2) {
						Clipper.PolyFillType.EVEN_ODD, Clipper.PolyFillType.NON_ZERO -> return windCnt2 != 0
						Clipper.PolyFillType.POSITIVE -> return windCnt2 > 0
						else -> return windCnt2 < 0
					}
				}
			}
			Clipper.ClipType.XOR -> if (windDelta == 0) {
				when (pft2) {
					Clipper.PolyFillType.EVEN_ODD, Clipper.PolyFillType.NON_ZERO -> return windCnt2 == 0
					Clipper.PolyFillType.POSITIVE -> return windCnt2 <= 0
					else -> return windCnt2 >= 0
				}
			} else {
				return true
			}
		}
	}

	fun isEvenOddAltFillType(clipFillType: Clipper.PolyFillType, subjFillType: Clipper.PolyFillType): Boolean {
		if (polyTyp == Clipper.PolyType.SUBJECT) {
			return clipFillType == Clipper.PolyFillType.EVEN_ODD
		} else {
			return subjFillType == Clipper.PolyFillType.EVEN_ODD
		}
	}

	fun isEvenOddFillType(clipFillType: Clipper.PolyFillType, subjFillType: Clipper.PolyFillType): Boolean {
		val type = (if (polyTyp == Clipper.PolyType.SUBJECT) subjFillType else clipFillType)
		return type == Clipper.PolyFillType.EVEN_ODD
	}

	val isHorizontal: Boolean get() = delta.y == 0.0

	fun isIntermediate(y: Double): Boolean = top.y == y && nextInLML != null
	fun isMaxima(Y: Double): Boolean = top.y == Y && nextInLML == null

	fun reverseHorizontal() {
		//swap horizontal edges' top and bottom x's so they follow the natural
		//progression of the bounds - ie so their xbots will align with the
		//adjoining lower edge. [Helpful in the ProcessHorizontal() method.]
		val temp = top.x
		top.x = bot.x
		bot.x = temp

		//temp = top.z
		//top.z = bot.z
		//bot.z = temp

	}


	override fun toString(): String = "TEdge [Bot=$bot, Curr=$current, Top=$top, Delta=$delta, Dx=$deltaX, PolyTyp=$polyTyp, Side=$side, WindDelta=$windDelta, WindCnt=$windCnt, WindCnt2=$windCnt2, OutIdx=$outIdx, Next=$next, Prev=$prev, NextInLML=$nextInLML, NextInAEL=$nextInAEL, PrevInAEL=$prevInAEL, NextInSEL=$nextInSEL, PrevInSEL=$prevInSEL]"

	fun updateDeltaX() {
		delta.x = top.x - bot.x
		delta.y = top.y - bot.y
		if (delta.y == 0.0) {
			deltaX = HORIZONTAL
		} else {
			deltaX = delta.x / delta.y
		}
	}

	companion object {
		fun doesE2InsertBeforeE1(e1: Edge, e2: Edge): Boolean = if (e2.current.x == e1.current.x) {
			if (e2.top.y > e1.top.y) (e2.top.x < topX(e1, e2.top.y)) else (e1.top.x > topX(e2, e1.top.y))
		} else {
			(e2.current.x < e1.current.x)
		}

		fun slopesEqual(e1: Edge, e2: Edge): Boolean = e1.delta.y * e2.delta.x == e1.delta.x * e2.delta.y

		fun swapPolyIndexes(edge1: Edge, edge2: Edge) {
			val outIdx = edge1.outIdx
			edge1.outIdx = edge2.outIdx
			edge2.outIdx = outIdx
		}

		fun swapSides(edge1: Edge, edge2: Edge) {
			val side = edge1.side
			edge1.side = edge2.side
			edge2.side = side
		}

		fun topX(edge: Edge, currentY: Double): Double {
			if (currentY == edge.top.y) return edge.top.x
			return (edge.bot.x + round(edge.deltaX * (currentY - edge.bot.y))).toInt().toDouble()
		}

		const val SKIP = -2
		const val UNASSIGNED = -1
		const val HORIZONTAL = -3.4E+38
	}
}

@Suppress("unused")
/**
 * A pure convenience class to avoid writing List<IntPoint> everywhere.

 * @author Tobias Mahlmann
</IntPoint> */
class Path private constructor(private val al: ArrayList<Point2d>) : MutableList<Point2d> by al, RandomAccess {
	constructor(initialCapacity: Int = 0) : this(ArrayList<Point2d>(initialCapacity))

	constructor(vararg points: Point2d) : this(points.size) {
		addAll(points)
	}

	constructor(points: List<Point2d>) : this(points.size) {
		addAll(points)
	}

	constructor(points: Iterable<Point2d>) : this() {
		addAll(points)
	}

	override fun toString(): String = al.toString()

	class Join(
		var outPt1: OutPt? = null,
		var outPt2: OutPt? = null,
		var offPt: Point2d? = null
	)

	fun addPoint(x: Double, y: Double) = add(Point2d(x, y))

	class OutPt {
		var idx: Int = 0
		var pt: Point2d = Point2d(0, 0)
		var next: OutPt? = null
		var prev: OutPt? = null

		fun duplicate(InsertAfter: Boolean): OutPt {
			val result = OutPt()
			result.pt = Point2d(pt)
			result.idx = idx
			if (InsertAfter) {
				result.next = next
				result.prev = this
				next!!.prev = result
				next = result
			} else {
				result.prev = prev
				result.next = this
				prev!!.next = result
				prev = result
			}
			return result
		}

		//there appears to be at least 2 vertices at bottomPt so ...
		val bottomPt: OutPt
			get() {
				var dups: OutPt? = null
				var p = next
				var pp = this
				while (p !== pp) {
					p!!
					if (p.pt.y > pp.pt.y) {
						pp = p
						dups = null
					} else if (p.pt.y == pp.pt.y && p.pt.x <= pp.pt.x) {
						if (p.pt.x < pp.pt.x) {
							dups = null
							pp = p
						} else {
							if (p.next !== pp && p.prev !== pp) {
								dups = p
							}
						}
					}
					p = p.next
				}
				if (dups != null) {
					while (dups !== p) {
						if (!isFirstBottomPt(p, dups!!)) {
							pp = dups
						}
						dups = dups.next
						while (dups!!.pt != pp.pt) {
							dups = dups.next
						}
					}
				}
				return pp
			}

		val pointCount: Int get() {
			var result = 0
			var p: OutPt? = this
			do {
				result++
				p = p!!.next
			} while (p !== this && p != null)
			return result
		}

		fun reversePolyPtLinks() {
			var pp1: OutPt
			var pp2: OutPt
			pp1 = this
			do {
				pp2 = pp1.next!!
				pp1.next = pp1.prev
				pp1.prev = pp2
				pp1 = pp2
			} while (pp1 !== this)
		}

		companion object {
			fun getLowerMostRec(outRec1: OutRec, outRec2: OutRec): OutRec {
				//work out which polygon fragment has the correct hole state ...
				outRec1.bottomPt = outRec1.bottomPt ?: outRec1.points!!.bottomPt
				outRec2.bottomPt = outRec2.bottomPt ?: outRec2.points!!.bottomPt
				val bPt1 = outRec1.bottomPt!!
				val bPt2 = outRec2.bottomPt!!
				return when {
					bPt1.pt.y > bPt2.pt.y -> outRec1
					bPt1.pt.y < bPt2.pt.y -> outRec2
					bPt1.pt.x < bPt2.pt.x -> outRec1
					bPt1.pt.x > bPt2.pt.x -> outRec2
					bPt1.next === bPt1 -> outRec2
					bPt2.next === bPt2 -> outRec1
					isFirstBottomPt(bPt1, bPt2) -> outRec1
					else -> outRec2
				}
			}

			private fun isFirstBottomPt(btmPt1: OutPt, btmPt2: OutPt): Boolean {
				var p: OutPt = btmPt1.prev!!
				while (p.pt == btmPt1.pt && p != btmPt1) p = p.prev!!
				val dx1p = abs(Points.getDeltaX(btmPt1.pt, p.pt))
				p = btmPt1.next!!
				while (p.pt == btmPt1.pt && p != btmPt1) p = p.next!!
				val dx1n = abs(Points.getDeltaX(btmPt1.pt, p.pt))
				p = btmPt2.prev!!
				while (p.pt == btmPt2.pt && p != btmPt2) p = p.prev!!
				val dx2p = abs(Points.getDeltaX(btmPt2.pt, p.pt))
				p = btmPt2.next!!
				while (p.pt == btmPt2.pt && p == btmPt2) p = p.next!!
				val dx2n = abs(Points.getDeltaX(btmPt2.pt, p.pt))
				return dx1p >= dx2p && dx1p >= dx2n || dx1n >= dx2p && dx1n >= dx2n
			}
		}
	}

	class OutRec {
		var Idx: Int = 0

		var isHole: Boolean = false

		var isOpen: Boolean = false
		var firstLeft: OutRec? = null //see comments in clipper.pas
		var points: OutPt? = null
		var bottomPt: OutPt? = null
		var polyNode: PolyNode? = null

		fun area(): Double {
			var op: OutPt? = points ?: return 0.0
			var a = 0.0
			do {
				a += (op!!.prev!!.pt.x + op.pt.x) * (op.prev!!.pt.y - op.pt.y)
				op = op.next
			} while (op !== points)
			return a * 0.5
		}

		fun fixHoleLinkage() {
			//skip if an outermost polygon or
			//already already points to the correct FirstLeft ...
			if (firstLeft == null || isHole != firstLeft!!.isHole && firstLeft!!.points != null) return
			var orfl = firstLeft
			while (orfl != null && (orfl.isHole == isHole || orfl.points == null)) orfl = orfl.firstLeft
			firstLeft = orfl
		}

		fun parseFirstLeft(): OutRec {
			var ret: OutRec? = this
			while (ret != null && ret.points == null) ret = ret.firstLeft
			return ret!!
		}
	}


	fun area(): Double {
		val cnt = size
		if (cnt < 3) {
			return 0.0
		}
		var a = 0.0
		var i = 0
		var j = cnt - 1
		while (i < cnt) {
			a += (get(j).x + get(i).x) * (get(j).y - get(i).y)
			j = i
			++i
		}
		return -a * 0.5
	}

	//@JvmOverloads fun cleanPolygon(distance: Double = 1.415): Path {
	fun cleanPolygon(distance: Double = 1.415): Path {
		//distance = proximity in units/pixels below which vertices will be stripped.
		//Default ~= sqrt(2) so when adjacent vertices or semi-adjacent vertices have
		//both x & y coords within 1 unit, then the second vertex will be stripped.

		var cnt = size

		if (cnt == 0) return Path()

		var outPts = Array(cnt) { OutPt() }

		for (i in 0 until cnt) {
			outPts[i].pt = get(i)
			outPts[i].next = outPts[(i + 1) % cnt]
			outPts[i].next!!.prev = outPts[i]
			outPts[i].idx = 0
		}

		val distSqrd = distance * distance
		var op = outPts[0]
		while (op.idx == 0 && op.next !== op.prev) {
			if (Points.arePointsClose(op.pt, op.prev!!.pt, distSqrd)) {
				op = excludeOp(op)
				cnt--
			} else if (Points.arePointsClose(op.prev!!.pt, op.next!!.pt, distSqrd)) {
				excludeOp(op.next!!)
				op = excludeOp(op)
				cnt -= 2
			} else if (Points.slopesNearCollinear(op.prev!!.pt, op.pt, op.next!!.pt, distSqrd)) {
				op = excludeOp(op)
				cnt--
			} else {
				op.idx = 1
				op = op.next!!
			}
		}

		if (cnt < 3) cnt = 0
		val result = Path(cnt)
		for (i in 0 until cnt) {
			result.add(op.pt)
			op = op.next!!
		}
		return result
	}

	fun isPointInPolygon(pt: Point2d): Int {
		//returns 0 if false, +1 if true, -1 if pt ON polygon boundary
		//See "The Point in Polygon Problem for Arbitrary Polygons" by Hormann & Agathos
		//http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.88.5498&rep=rep1&type=pdf
		var result = 0
		val cnt = size
		if (cnt < 3) return 0
		var ip = get(0)
		for (i in 1..cnt) {
			val ipNext = if (i == cnt) get(0) else get(i)
			if (ipNext.y == pt.y) {
				if (ipNext.x == pt.x || ip.y == pt.y && ipNext.x > pt.x == ip.x < pt.x) return -1
			}
			if (ip.y < pt.y != ipNext.y < pt.y) {
				if (ip.x >= pt.x) {
					if (ipNext.x > pt.x) {
						result = 1 - result
					} else {
						val d = (ip.x - pt.x) * (ipNext.y - pt.y) - (ipNext.x - pt.x) * (ip.y - pt.y)
						if (d == 0.0) {
							return -1
						} else if (d > 0 == ipNext.y > ip.y) {
							result = 1 - result
						}
					}
				} else {
					if (ipNext.x > pt.x) {
						val d = (ip.x - pt.x) * (ipNext.y - pt.y) - (ipNext.x - pt.x) * (ip.y - pt.y)
						if (d == 0.0) {
							return -1
						} else if (d > 0 == ipNext.y > ip.y) {
							result = 1 - result
						}
					}
				}
			}
			ip = ipNext
		}
		return result
	}

	fun orientation(): Boolean = area() >= 0

	fun translatePath(delta: Point2d): Path {
		val outPath = Path(size)
		for (i in this) outPath.add(Point2d(i.x + delta.x, i.y + delta.y))
		return outPath
	}

	companion object {
		private fun excludeOp(op: OutPt): OutPt {
			val result = op.prev
			result!!.next = op.next
			op.next!!.prev = result
			result.idx = 0
			return result
		}
	}
}

@Suppress("unused")
/**
 * A pure convenience class to avoid writing List<Path> everywhere.

 * @author Tobias Mahlmann
</Path> */
class Paths private constructor(private val al: ArrayList<Path>) : MutableList<Path> by al {
	constructor() : this(arrayListOf()) {}

	constructor(initialCapacity: Int) : this(ArrayList(initialCapacity)) {}
	constructor(vararg items: Path) : this(arrayListOf()) {
		addAll(items)
	}

	constructor(items: Iterable<Path>) : this(arrayListOf()) {
		addAll(items)
	}

	override fun toString(): String = al.toString()

	val totalVertices: Int get() = this.sumBy { it.size }

	fun addPolyNode(polynode: PolyNode, nt: PolyNode.NodeType) {
		var match = true
		when (nt) {
			PolyNode.NodeType.OPEN -> return
			PolyNode.NodeType.CLOSED -> match = !polynode.isOpen
			else -> Unit
		}

		if (polynode.polygon.size > 0 && match) add(polynode.polygon)
		for (pn in polynode.getChilds()) addPolyNode(pn, nt)
	}

	//@JvmOverloads fun cleanPolygons(distance: Double = 1.415): Paths {
	fun cleanPolygons(distance: Double = 1.415): Paths {
		val result = Paths(size)
		for (i in 0 until size) result.add(get(i).cleanPolygon(distance))
		return result
	}

	val bounds: Rectangle get() {
		//var i = 0
		//val cnt = size
		//val result = Rectangle()
		//while (i < cnt && get(i).isEmpty()) {
		//	i++
		//}
		//if (i == cnt) {
		//	return result
		//}
//
		//result.left = get(i)[0].x
		//result.right = result.left
		//result.top = get(i)[0].y
		//result.bottom = result.top
		//while (i < cnt) {
		//	for (j in 0..get(i).size - 1) {
		//		if (get(i)[j].x < result.left) {
		//			result.left = get(i)[j].x
		//		} else if (get(i)[j].x > result.right) {
		//			result.right = get(i)[j].x
		//		}
		//		if (get(i)[j].y < result.top) {
		//			result.top = get(i)[j].y
		//		} else if (get(i)[j].y > result.bottom) {
		//			result.bottom = get(i)[j].y
		//		}
		//	}
		//	i++
		//}
		//return result
		val bb = BoundsBuilder()
		for (path in this) for (p in path) bb.add(p)
		return bb.getBounds()
	}

	fun reversePaths() = run { for (poly in this) poly.reverse() }

	companion object {
		fun closedPathsFromPolyTree(polytree: PolyTree): Paths = Paths().apply { addPolyNode(polytree, PolyNode.NodeType.CLOSED) }
		fun makePolyTreeToPaths(polytree: PolyTree): Paths = Paths().apply { addPolyNode(polytree, PolyNode.NodeType.ANY) }
		fun openPathsFromPolyTree(polytree: PolyTree): Paths = Paths(polytree.getChilds().filter { it.isOpen }.map { it.polygon })
	}
}

object Points {
	fun arePointsClose(pt1: Point2d, pt2: Point2d, distSqrd: Double): Boolean {
		val dx = pt1.x - pt2.x
		val dy = pt1.y - pt2.y
		return dx * dx + dy * dy <= distSqrd
	}

	fun distanceFromLineSqrd(pt: Point2d, ln1: Point2d, ln2: Point2d): Double {
		//The equation of a line in general form (Ax + By + C = 0)
		//given 2 points (x�,y�) & (x�,y�) is ...
		//(y� - y�)x + (x� - x�)y + (y� - y�)x� - (x� - x�)y� = 0
		//A = (y� - y�); B = (x� - x�); C = (y� - y�)x� - (x� - x�)y�
		//perpendicular distance of point (x�,y�) = (Ax� + By� + C)/Sqrt(A� + B�)
		//see http://en.wikipedia.org/wiki/Perpendicular_distance
		val A = ln1.y - ln2.y
		val B = ln2.x - ln1.x
		var C = A * ln1.x + B * ln1.y
		C = A * pt.x + B * pt.y - C
		return C * C / (A * A + B * B)
	}

	fun getDeltaX(pt1: Point2d, pt2: Point2d): Double = if (pt1.y == pt2.y) Edge.HORIZONTAL else (pt2.x - pt1.x) / (pt2.y - pt1.y)

	fun getUnitNormal(pt1: Point2d, pt2: Point2d): Point2d {
		val dx = (pt2.x - pt1.x)
		val dy = (pt2.y - pt1.y)
		if (dx == 0.0 && dy == 0.0) return Point2d()
		val f = 1 * 1.0 / sqrt(dx * dx + dy * dy)
		return Point2d(dy * f, -dx * f)
	}

	fun isPt2BetweenPt1AndPt3(pt1: Point2d, pt2: Point2d, pt3: Point2d): Boolean = when {
		(pt1 == pt3 || pt1 == pt2 || pt3 == pt2) -> false
		(pt1.x != pt3.x) -> (pt2.x > pt1.x == pt2.x < pt3.x)
		else -> (pt2.y > pt1.y == pt2.y < pt3.y)
	}

	fun slopesEqual(pt1: Point2d, pt2: Point2d, pt3: Point2d): Boolean = (pt1.y - pt2.y) * (pt2.x - pt3.x) - (pt1.x - pt2.x) * (pt2.y - pt3.y) == 0.0

	fun slopesEqual(pt1: Point2d, pt2: Point2d, pt3: Point2d, pt4: Point2d): Boolean = (pt1.y - pt2.y) * (pt3.x - pt4.x) - (pt1.x - pt2.x) * (pt3.y - pt4.y) == 0.0

	fun slopesNearCollinear(pt1: Point2d, pt2: Point2d, pt3: Point2d, distSqrd: Double): Boolean {
		//this function is more accurate when the point that's GEOMETRICALLY
		//between the other 2 points is the one that's tested for distance.
		//nb: with 'spikes', either pt1 or pt3 is geometrically between the other pts
		return if (abs(pt1.x - pt2.x) > abs(pt1.y - pt2.y)) {
			when {
				(pt1.x > pt2.x == pt1.x < pt3.x) -> distanceFromLineSqrd(pt1, pt2, pt3) < distSqrd
				(pt2.x > pt1.x == pt2.x < pt3.x) -> distanceFromLineSqrd(pt2, pt1, pt3) < distSqrd
				else -> distanceFromLineSqrd(pt3, pt1, pt2) < distSqrd
			}
		} else {
			when {
				(pt1.y > pt2.y == pt1.y < pt3.y) -> distanceFromLineSqrd(pt1, pt2, pt3) < distSqrd
				(pt2.y > pt1.y == pt2.y < pt3.y) -> distanceFromLineSqrd(pt2, pt1, pt3) < distSqrd
				else -> distanceFromLineSqrd(pt3, pt1, pt2) < distSqrd
			}
		}
	}
}

open class PolyNode {
	enum class NodeType { ANY, OPEN, CLOSED }

	var parent: PolyNode? = null
	val polygon = Path()
	private var index: Int = 0
	var joinType: Clipper.JoinType? = null
	var endType: Clipper.EndType? = null
	val _childs: MutableList<PolyNode> = ArrayList()
	var isOpen: Boolean = false

	fun addChild(child: PolyNode) {
		val cnt = _childs.size
		_childs.add(child)
		child.parent = this
		child.index = cnt
	}

	val childCount: Int get() = _childs.size
	fun getChilds(): List<PolyNode> = _childs.toList()
	val contour: List<Point2d> get() = polygon

	val next: PolyNode get() = if (!_childs.isEmpty()) _childs[0] else nextSiblingUp!!
	private val nextSiblingUp: PolyNode? get() = if (parent == null) null else if (index == parent!!._childs.size - 1) parent!!.nextSiblingUp else parent!!._childs[index + 1]

	val isHole: Boolean get() = isHoleNode

	private val isHoleNode: Boolean get() {
		var result = true
		var node = parent
		while (node != null) {
			result = !result
			node = node.parent
		}
		return result
	}
}

class PolyTree : PolyNode() {
	val allPolys = ArrayList<PolyNode>()

	fun Clear() {
		allPolys.clear()
		_childs.clear()
	}

	val first: PolyNode? get() = if (!_childs.isEmpty()) _childs[0] else null

	//with negative offsets, ignore the hidden outer polygon ...
	val totalSize: Int get() {
		var result = allPolys.size
		return if (result > 0 && _childs[0] !== allPolys[0]) result - 1 else result
	}
}

package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point2d

class SegmentEmitter {
	@PublishedApi internal val p1 = Point2d()
	@PublishedApi internal val p2 = Point2d()

	inline fun emit(segments: Int, crossinline curveGen: (p: Point2d, t: Double) -> Point2d, crossinline gen: (p0: Point2d, p1: Point2d) -> Unit) = synchronized(this) {
		val dt = 1.0 / segments
		for (n in 0 until segments) {
			p1.copyFrom(p2)
			p2.copyFrom(curveGen(p2, dt * n))
			if (n > 1) gen(p1, p2)
		}
	}
}
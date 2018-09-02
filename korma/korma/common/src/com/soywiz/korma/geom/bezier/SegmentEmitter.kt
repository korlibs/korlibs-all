package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.*
import com.soywiz.std.*

object SegmentEmitter {
	inline fun emit(
		segments: Int,
		crossinline curveGen: (p: MPoint2d, t: Double) -> MPoint2d,
		crossinline gen: (p0: MPoint2d, p1: MPoint2d) -> Unit,
		p1: MPoint2d = MPoint2d(),
		p2: MPoint2d = MPoint2d()
	) = synchronized2(this) {
		val dt = 1.0 / segments
		for (n in 0 until segments) {
			p1.copyFrom(p2)
			p2.copyFrom(curveGen(p2, dt * n))
			if (n > 1) gen(p1, p2)
		}
	}
}
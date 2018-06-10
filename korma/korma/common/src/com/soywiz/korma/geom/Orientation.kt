package com.soywiz.korma.geom

import com.soywiz.korma.geom.triangle.*

enum class Orientation(val value: Int) {
	CW(+1), CCW(-1), COLLINEAR(0);

	companion object {
		fun orient2d(pa: Point2d, pb: Point2d, pc: Point2d): Orientation {
			val detleft: Double = (pa.x - pc.x) * (pb.y - pc.y)
			val detright: Double = (pa.y - pc.y) * (pb.x - pc.x)
			val `val`: Double = detleft - detright

			if ((`val` > -Constants.EPSILON) && (`val` < Constants.EPSILON)) return Orientation.COLLINEAR
			if (`val` > 0) return Orientation.CCW
			return Orientation.CW
		}
	}
}

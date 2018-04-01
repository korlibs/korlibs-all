package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.*

fun Bezier.length(steps: Int = 100): Double {
	val dt = 1.0 / steps.toDouble()
	return (0 until steps).map { calc(dt * it) }.getPolylineLength()
}

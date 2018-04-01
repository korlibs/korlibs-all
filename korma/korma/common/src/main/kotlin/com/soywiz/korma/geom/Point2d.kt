package com.soywiz.korma.geom

import com.soywiz.korma.IVector2
import com.soywiz.korma.Vector2
import com.soywiz.korma.distanceTo

typealias IPoint2d = IVector2
typealias Point2d = Vector2

// @TODO: Check if this avoid boxing!
inline fun Point2d(x: Number, y: Number) = Point2d(x.toDouble(), y.toDouble())

inline fun IPoint2d(x: Number, y: Number) = IVector2(x.toDouble(), y.toDouble())

fun Iterable<IPoint2d>.getPolylineLength(): Double {
	var out = 0.0
	var prev: IPoint2d? = null
	for (cur in this) {
		if (prev != null) out += prev.distanceTo(cur)
		prev = cur
	}
	return out
}